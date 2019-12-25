package wiki.dig.algorithm.keyword

import java.io.FileReader

import breeze.io.CSVReader
import ruc.irm.extractor.keyword.graph.PositionWordGraph
import wiki.dig.util.DotFile

import scala.collection.mutable
import scala.jdk.CollectionConverters._

/**
  * 中文图情领域期刊的标题、摘要和关键词数据集
  */
object PaperDataset {
  val papers: IndexedSeq[Paper] = {
    val reader = new FileReader("./data/paper_abstract.csv")
    val csvReader = CSVReader.read(reader, skipLines = 1)
    val results = csvReader.map {
      r =>
        Paper(r(0), r(1).split(";"), r(2))
    }
    reader.close()
    results
  }

  def count() = papers.length

  def get(idx: Int) = papers(idx)

  def main(args: Array[String]): Unit = {
    papers.foreach(p => println(p.title))
  }


  def toDotFile(id: Int, dotFile: String): Unit = {
    val paper = get(id)
    val g = new PositionWordGraph(1, 0, 0, true)
    g.build(paper.title, 30)
    g.build(paper.`abstract`, 1.0f)

    val pairSet = mutable.Set.empty[String]

    val triples: Seq[(String, String, Int)] = g.getWordNodeMap.asScala.toSeq
      .sortBy(_._2.getCount)(Ordering.Int.reverse)
      .take(100)
      .flatMap {
        case (name, node) =>
          //转换为二元组对：（词语，词语右侧相邻的词语）
          val pairs1: Seq[(String, String, Int)] = node.getLeftNeighbors.asScala.map {
            case (adjName, cnt) =>
              (adjName, name, cnt.toInt)
          }.toSeq

          val pairs2: Seq[(String, String, Int)] = node.getRightNeighbors.asScala.map {
            case (adjName, cnt) =>
              (name, adjName, cnt.toInt)
          }.toSeq

          pairs1 ++: pairs2
      }

    DotFile.toDotFile(triples, dotFile)
  }
}

case class Paper(title: String, tags: Seq[String], `abstract`: String)
