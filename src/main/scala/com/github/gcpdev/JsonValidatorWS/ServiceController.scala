package com.github.gcpdev.JsonValidatorWS

import java.net.URLDecoder
import javax.sql.DataSource

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.github.fge.jsonschema.core.report.{ProcessingMessage, ProcessingReport}
import com.github.fge.jsonschema.main.{JsonSchema, JsonSchemaFactory}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.{HttpHeaders, HttpStatus, ResponseEntity}
import org.springframework.web.bind.annotation.RequestMethod._
import org.springframework.web.bind.annotation._
import spray.json._

import scala.annotation.tailrec
import scala.collection.GenTraversable




@RestController
@RequestMapping(path = Array("/"))
class ServiceController(@Autowired val schemaService: SchemaService, @Autowired val dataSource: DataSource) extends DefaultJsonProtocol {

  //download path: returns the schema with specific schemaId
  @GetMapping(path = Array("/schema/{schemaId}"), produces = Array("application/json"))
  def getSchema(@PathVariable schemaId: String): Schema = {
    schemaService.getSchema(schemaId)
  }

  //upload path: handles PUT/POST actions in the /schema/{schemaId} path
  @RequestMapping(path = Array("/schema/{schemaId}"), method = Array(PUT, POST), consumes = Array("text/plain", "application/json","application/x-www-form-urlencoded"), produces = Array("application/json"))
  @ResponseStatus(HttpStatus.CREATED)
  def createSchema(@RequestBody schemaRequest: String, @PathVariable schemaId: String): ResponseEntity[String] = {
    val schema: Schema = new Schema()

    //URLDecoder is necessary to decode requests that doesn't contain proper "Content-type" in the Header
    var schemaInput: String = URLDecoder.decode(schemaRequest, "UTF-8")

    var status: String = "success" //hopefully
    var httpReturnStatus: HttpStatus = HttpStatus.CREATED

    var message: String = ""

    //try to validate and save the schema successfully, or get error message
    try {
      //validates Json input
      implicit val validate = schemaInput.parseJson

      schema.setSchema(schemaInput) //set schema content
      schema.setSchemaId(schemaId) //set schema id

      //saves the schema
      val schemaSaved: String = schemaService.createSchema(schema)
    }
    catch {
      //handles any kind of errors
      case unknown: Throwable => message = unknown.getMessage()
                                 status = "error"
                                 httpReturnStatus = HttpStatus.BAD_REQUEST //uh-oh
    }

    //jsonResponse is a Map, which is easily converted toJson with spray.json
      var jsonResponse = Map(
        "action" -> "uploadSchema",
        "id" -> schemaId)

    if(status != "success") {
      jsonResponse += "status" -> "error"
      jsonResponse += "message" -> message
    }

    else jsonResponse += "status" -> status

    new ResponseEntity(jsonResponse.toJson.compactPrint, new HttpHeaders, httpReturnStatus)
  }


  //validation path: handles PUT/POST actions in the /validate/{schemaId} path
  @PostMapping(path = Array("/validate/{schemaId}"))
  def validateSchema(@RequestBody json: String, @PathVariable schemaId: String): ResponseEntity[String] = {
    var jsonResponse = Map(
      "action" -> "validateDocument",
      "id" -> schemaId)

    //URLDecoder is necessary to decode requests that doesn't contain proper "Content-type" in the Header
    var jsonInput = URLDecoder.decode(json, "UTF-8")

    var status: String = "success" //hopefully
    var httpReturnStatus: HttpStatus = HttpStatus.OK

    var message: String = ""

    //try to validate the Json input and then validate against the schema successfully, or get error message
    try {

      val validJsonInput = jsonInput.parseJson // if Json is not good, parse throws exception

      //filter null values
      def nullValue(v: JsValue) = v match {
        case JsNull => true
        case _ => false
      }
      def cleanJsonInput(json: JsValue): JsValue = json match{
        case JsObject(fields) =>
            JsObject(fields.filterNot(t => nullValue(t._2)))
        case other => other
      }
      var cleanedJsonValue = cleanJsonInput(validJsonInput)

      //gets the validator schema
      val valSchema: String = schemaService.getSchema(schemaId).schema

      //validates against schema
      val mapper = new ObjectMapper
      //first converts the schema to a JsonNode
      var jsonObj: JsonNode = mapper.readTree(valSchema)
      val schemaFactory: JsonSchemaFactory = JsonSchemaFactory.byDefault()

      val schema: JsonSchema = schemaFactory.getJsonSchema(jsonObj)

      //now converts the json input to a JsonNode
      jsonObj = mapper.readTree(cleanedJsonValue.compactPrint)

      val report: ProcessingReport = schema.validate(jsonObj)

      //if has errors...
      if(!report.isSuccess()) {
        var text: String = ""
        var message: ProcessingMessage = null
        val itr = report.iterator

        //strip error messages out of the report and concatenate these errors
        var errors: Int = 0
        while ({ itr.hasNext }) {
          errors += 1
          message = itr.next.asInstanceOf[ProcessingMessage]
          text += errors + ". " + message.asJson.get("message").asText()
        }
        //throw exception with error messages
        throw new RuntimeException(text)
      }
      System.out.println(report)
    }
    catch {
      case unknown: Throwable => message = unknown.getMessage()
        status = "error"
        httpReturnStatus = HttpStatus.BAD_REQUEST //uh-oh
    }

    if(status != "success") {
      jsonResponse += "status" -> "error"
      jsonResponse += "message" -> message
    }

    else jsonResponse += "status" -> status

    new ResponseEntity(jsonResponse.toJson.compactPrint, new HttpHeaders, httpReturnStatus)
 }

}
