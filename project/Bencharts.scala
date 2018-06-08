import javax.imageio.ImageIO
import org.jfree.chart.JFreeChart
import org.jfree.chart.axis.LogarithmicAxis
import org.jfree.chart.plot.XYPlot
import org.jfree.chart.renderer.xy.XYErrorRenderer
import org.jfree.data.xy.{YIntervalSeries, YIntervalSeriesCollection}
import sbt._
import com.github.plokhotnyuk.jsoniter_scala.macros._
import com.github.plokhotnyuk.jsoniter_scala.core._

import scala.collection.SortedMap

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
    allResults.groupBy(benchmarkName).foreach { case (benchmark, results) =>
        val seriess = SortedMap(results.groupBy(otherParams).toSeq:_*)
          .map { case (params, iterations) =>
            val ySeries = new YIntervalSeries(params)
            // each benchmark has been run with several sizes (10, 100, 1000, etc.)
            // we add a point for each of these iterations
            iterations.foreach { iteration =>
              ySeries.add(
                iteration.params.get("size").fold(0.0)(_.toDouble),
                iteration.primaryMetric.score,
                iteration.primaryMetric.scoreConfidence._1,
                iteration.primaryMetric.scoreConfidence._2
              )
            }
            ySeries
          }
        val plot = {
          val xAxis = new LogarithmicAxis("Size")
          val yAxis = new LogarithmicAxis(yAxisTitle)
          val col = new YIntervalSeriesCollection
          val renderer = new XYErrorRenderer
          seriess.zipWithIndex.foreach { case (series, i) =>
            col.addSeries(series)
            renderer.setSeriesLinesVisible(i, true)
          }
          new XYPlot(col, xAxis, yAxis, renderer)
        }
        val chart = new JFreeChart(benchmark, JFreeChart.DEFAULT_TITLE_FONT, plot, true)
        ImageIO.write(chart.createBufferedImage(1024, 768), "png", targetDir / s"$benchmark.png")
      }
  }

  private def benchmarkName(result: BenchmarkResult): String =
    result.benchmark.split("""\.""").last

  private def otherParams(result: BenchmarkResult): String = {
    val benchSuitName = result.benchmark.split("""\.""").reverse.tail.head
    result.params.filterKeys(_ != "size").map { case (k, v) =>
      s"$k=$v"
    }.toSeq.sorted.mkString(s"$benchSuitName[", ",", "]")
  }
}
