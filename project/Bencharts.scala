import java.awt.{Color, Paint}
import java.text.NumberFormat

import javax.imageio.ImageIO
import org.jfree.chart.JFreeChart
import org.jfree.chart.axis.LogarithmicAxis
import org.jfree.chart.plot.{DefaultDrawingSupplier, XYPlot}
import org.jfree.chart.renderer.xy.XYErrorRenderer
import org.jfree.data.xy.{YIntervalSeries, YIntervalSeriesCollection}
import sbt._
import com.github.plokhotnyuk.jsoniter_scala.macros._
import com.github.plokhotnyuk.jsoniter_scala.core._

import scala.collection.{SortedMap, immutable}

case class BenchmarkMetric(score: Double, scoreConfidence: (Double, Double))

case class BenchmarkResult(benchmark: String, params: Map[String, String], primaryMetric: BenchmarkMetric)

object Bencharts {
  implicit val codec: JsonValueCodec[Seq[BenchmarkResult]] = JsonCodecMaker.make[Seq[BenchmarkResult]](CodecMakerConfig())

  /**
    * Generate charts from the result of a JMH execution.
    *
    * Benchmarks that have the same name (e.g. 'apply') are grouped into a single chart
    * with one series of several size param values for each value combination of other params.
    *
    * @param jmhReport JMH results report
    * @param targetDir Directory in which the images will be written
    */
  def apply(jmhReport: File, yAxisTitle: String, targetDir: File): Unit = {
    val allResults = readFromArray(IO.readBytes(jmhReport))
    val constParams = allResults.flatMap(_.params.toSeq).groupBy(_._1)
      .collect { case (k, kvs) if kvs.distinct.size == 1 => kvs.head }.toSeq
    allResults.groupBy(benchmarkName(constParams)).foreach { case (benchmark, results) =>
      val seriess = SortedMap(results.groupBy(otherParams(constParams)).toSeq:_*)
        .map { case (params, iterations) =>
          val ySeries = new YIntervalSeries(params)
          // each benchmark has been run with several sizes (10, 100, 1000, etc.)
          // we add a point for each of these iterations
          iterations.foreach { iteration =>
            ySeries.add(
              iteration.params.get("size").fold(0.0)(_.toDouble),
              Math.max(iteration.primaryMetric.score, 1.0),
              Math.max(iteration.primaryMetric.scoreConfidence._1, 1.0),
              Math.max(iteration.primaryMetric.scoreConfidence._2, 1.0)
            )
          }
          ySeries
        }
      val col = new YIntervalSeriesCollection
      val renderer = new XYErrorRenderer
      seriess.zipWithIndex.foreach { case (series, i) =>
        col.addSeries(series)
        renderer.setSeriesLinesVisible(i, true)
      }
      val plot = new XYPlot(col, axis("Size"), axis(yAxisTitle), renderer)
      plot.setDrawingSupplier(new DefaultDrawingSupplier {
        override def getNextPaint: Paint = super.getNextPaint match {
          case x: Color if x.getRed > 200 && x.getGreen > 200 =>
            new Color(x.getRed, (x.getGreen * 0.8).toInt, x.getBlue, x.getAlpha)
          case x => x
        }
      })
      val chart = new JFreeChart(benchmark, JFreeChart.DEFAULT_TITLE_FONT, plot, true)
      ImageIO.write(chart.createBufferedImage(1200, 900), "png", targetDir / s"$benchmark.png")
    }
  }

  private def axis(title: String) = new LogarithmicAxis(title) {
    setAllowNegativesFlag(true)
    setNumberFormatOverride(NumberFormat.getInstance())
  }

  private def benchmarkName(constParams: Seq[(String, String)])(result: BenchmarkResult): String = {
    val benchName = result.benchmark.split("""\.""").last
    constParams.map { case (k, v) =>
      s"$k=$v"
    }.sorted.mkString(s"$benchName[", ",", "]")
  }

  private def otherParams(constParams: Seq[(String, String)])(result: BenchmarkResult): String = {
    val constParamNames = constParams.map(_._1).toSet
    val benchSuitName = result.benchmark.split("""\.""").reverse.tail.head
    result.params.filterKeys(k => k != "size" && !constParamNames(k)).map { case (k, v) =>
      s"$k=$v"
    }.toSeq.sorted.mkString(s"$benchSuitName[", ",", "]")
  }
}
