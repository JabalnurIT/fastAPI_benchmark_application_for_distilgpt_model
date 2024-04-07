import scala.language.postfixOps
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.event.slf4j.Logger
import akka.http.javadsl.TimeoutAccess
import akka.http.scaladsl.Http
import akka.http.scaladsl.TimeoutAccess
import akka.http.scaladsl.settings.{ClientConnectionSettings, ConnectionPoolSettings}
import com.typesafe.config.ConfigFactory
import spray.json.DefaultJsonProtocol.{IntJsonFormat, StringJsonFormat, mapFormat}
import spray.json.{JsString, JsValue, JsonParser, enrichAny}

import java.io.PrintWriter
import scala.math.pow
import java.io.File
//import akka.http.scaladsl.model.HttpMessage.AlreadyDiscardedEntity.future
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest}

import akka.http.scaladsl.unmarshalling.Unmarshal
//import akka.stream.ActorMaterializer

//import java.net.URLEncoder
import scala.concurrent.Await
import scala.concurrent.duration._

import scala.io.Source

object Main {

  private val appName = "main_app"
  private val logger = Logger(appName)



  implicit val system: ActorSystem = ActorSystem("system")
  //  implicit val materializer: ActorMaterializer = ActorMaterializer()
  import system.dispatcher


  val settings: ConnectionPoolSettings = ConnectionPoolSettings(system)
    .withConnectionSettings(ClientConnectionSettings(system)
      .withIdleTimeout(Duration.Inf))
//  implicit val baseURL: String = "http://18.136.194.164:8000"
  implicit val baseURL: String = "http://127.0.0.1:8000"


  implicit val basePath: String = "src/assets"

  private def makeDirsFromList(dir: String, dirs: List[String]): Unit = {
    dirs.foreach { d =>
      val newDir = new File(dir + "/" + d)
      newDir.mkdir()
    }
  }

  private def readAllFilesInDir(dir: String): List[String] = {
    val directory = new File(dir)
    if (directory.exists() && directory.isDirectory) {
      directory.listFiles()
        .filter(_.isFile)
        .map(_.getName)
        .toList
    } else {
      List.empty[String]
    }
  }

  private def readAllDirsInDir(dir: String): List[String] = {
    val directory = new File(dir)
    if (directory.exists() && directory.isDirectory) {
      directory.listFiles()
        .filter(_.isDirectory)
        .map(_.getName)
        .toList
    } else {
      List.empty[String]
    }
  }

  private def fileToList(filePath: String): List[String] = {
    val source = Source.fromFile(filePath)
    var listDataset = List[String]()
    try {
      listDataset = source.getLines().toList
    } catch {
      case e: Exception =>
        logger.error(s"${e.getMessage}")
    }
    finally {
      source.close()
    }

    listDataset
  }

  private def listToFile(filePath: String, list: List[String]): Unit = {
    val writer = new PrintWriter(filePath)
    try {
      list.foreach(item => writer.println(item))
      logger.info("Data saved to CSV file successfully.")
    } catch  {
      case e: Exception =>
        logger.error(s"${e.getMessage}")
    }
    finally {
      writer.close()
    }
  }

  private def getModelListRequest(): (String, List[String]) = {
    val requestModelList = HttpRequest(
      method = HttpMethods.POST,
      uri = s"$baseURL/model-list"
    )
    val futureRes = for {
      resp <- Http().singleRequest(requestModelList, settings = settings)
      res <- Unmarshal(resp.entity).to[String]
    } yield res

    val res = Await.result(futureRes, Duration.Inf)
    val jsonRes: JsValue = JsonParser(res)

    val currentModel = jsonRes.asJsObject
      .fields("current")
      .toString()
      .replace("\"","")
    val modelList = jsonRes.asJsObject
      .fields("models")
      .toString()
      .replaceAll("\\[|\\]|\"","")
      .split(",")
      .toList

    (currentModel, modelList)
  }

  private def useModelRequest(model: String): String = {
    val requestModelList = HttpRequest(
      method = HttpMethods.POST,
      uri = s"$baseURL/use-model",
      entity = HttpEntity(
        ContentTypes.`application/json`,
        Map("model" -> model).toJson.toString()
      )
    )
    val futureRes = for {
      resp <- Http().singleRequest(requestModelList, settings = settings)
      res <- Unmarshal(resp.entity).to[String]
    } yield res

    val res = Await.result(futureRes, Duration.Inf)
    val jsonRes: JsValue = JsonParser(res)

    val status = jsonRes.asJsObject
      .fields("status")
      .toString()
      .replace("\"", "")

    status
  }

  private def retrainRequest(text: String): String = {
    val requestRetrain = HttpRequest(
      method = HttpMethods.POST,
      uri = s"$baseURL/retrain",
      entity = HttpEntity(
        ContentTypes.`application/json`,
        Map("texts" -> text).toJson.toString()
      )
    )
    val futureRes = for {
      resp <- Http().singleRequest(requestRetrain, settings = settings)
      res <- Unmarshal(resp.entity).to[String]
    } yield res

    val res = Await.result(futureRes, Duration.Inf)
    val jsonRes: JsValue = JsonParser(res)

    val status = jsonRes.asJsObject
      .fields("status")
      .toString()
      .replace("\"", "")

    status
  }
  private def generateInputRequest(count: Int): List[String] = {
    val requestGenerateInput = HttpRequest(
      method = HttpMethods.POST,
      uri = s"$baseURL/generate",
      entity = HttpEntity(
        ContentTypes.`application/json`,
        Map("num_text" -> count).toJson.toString()
      )
    )
    val futureRes = for {
      resp <- Http().singleRequest(requestGenerateInput, settings = settings)
      res <- Unmarshal(resp.entity).to[String]
    } yield res

    val res = Await.result(futureRes, Duration.Inf)
    val jsonRes: JsValue = JsonParser(res)

    val dataset = jsonRes.asJsObject
      .fields("dataset")
      .toString()
      .replace("\"", "")

    val listDataset = dataset.split("###").toList
    listDataset
  }

  private def resetModelRequest(): String = {
    val requestRetrain = HttpRequest(
      method = HttpMethods.POST,
      uri = s"$baseURL/reset",
    )
    val futureRes = for {
      resp <- Http().singleRequest(requestRetrain, settings = settings)
      res <- Unmarshal(resp.entity).to[String]
    } yield res

    val res = Await.result(futureRes, Duration.Inf)
    val jsonRes: JsValue = JsonParser(res)

    val status = jsonRes.asJsObject
      .fields("status")
      .toString()
      .replace("\"", "")

    status
  }

  private def deleteModelRequest(): String = {
    val requestRetrain = HttpRequest(
      method = HttpMethods.POST,
      uri = s"$baseURL/delete",
    )
    val futureRes = for {
      resp <- Http().singleRequest(requestRetrain, settings = settings)
      res <- Unmarshal(resp.entity).to[String]
    } yield res

    val res = Await.result(futureRes, Duration.Inf)
    val jsonRes: JsValue = JsonParser(res)

    val status = jsonRes.asJsObject
      .fields("status")
      .toString()
      .replace("\"", "")

    status
  }

  def main(args: Array[String]): Unit = {


//    val list = List("age_analysis", "commute_type", "commute_type_full",
//      "customers", "delays", "delivery_faults", "external_call",
//      "find_salary", "flight_distance", "income_aggregation",
//      "inside_circle", "loan_type", "movie_rating",
//      "number_series", "student_grade",
//      "word_count")
    val list = List("commute_type_full")
//  ,
//      "customers", "flight_distance")
//    makeDirsFromList("src/assets/",list)

//    val programs = readAllDirsInDir("src/assets/").sorted
    val programs = list
    programs.foreach({ appName =>
      logger.info(appName)
//      if(readAllFilesInDir(s"src/assets/$appName/output/").sorted == List[String]()){
        val useModelStatus = useModelRequest(appName)
        val (currentModel, modelList) = getModelListRequest()
        logger.info(s"current model: $currentModel")
        logger.info(s"current model: $modelList")
        if (useModelStatus != "Success"){
          logger.error(useModelStatus)
          system.terminate()
        }
        println(s"use model status: $useModelStatus")

        val allData = fileToList(s"$basePath/$appName/input/incorrect_data.csv")
        println(s"Total Data: ${allData.length}")
        for (i <- 0 until math.min(5, allData.length)) {
          println(allData(i))
        }
        println("-- Training --")
        val retrainStatus = retrainRequest(allData.mkString("###"))
        if (retrainStatus != "Success") {
          logger.error(retrainStatus)
          system.terminate()
        }
        println(s"retrain status: $retrainStatus")

        for (i <- 0 until 6) {
          val numRow: Int = pow(2,i).toInt
          println(s"Num of row: $numRow")
          val newDataset = generateInputRequest(numRow)
          newDataset.foreach(println)
          listToFile(s"$basePath/$appName/output/new_incorrect_dataset_$numRow.csv",newDataset)
          println()
        }

//        val resetModelStatus = resetModelRequest()
//        if (resetModelStatus != "Success") {
//          logger.error(resetModelStatus)
//          system.terminate()
//        }
//        println(s"reset model status: $resetModelStatus")
//
//        val deleteModelStatus = deleteModelRequest()
//        if (deleteModelStatus != "Success") {
//          logger.error(deleteModelStatus)
//          system.terminate()
//        }
//        println(s"delete model status: $deleteModelStatus")
//      }
    })


    system.terminate()

  }
}