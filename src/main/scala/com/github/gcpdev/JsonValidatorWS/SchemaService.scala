package com.github.gcpdev.JsonValidatorWS

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import scala.collection.JavaConverters._

@Service
  class SchemaService(@Autowired private val schemaRepository: SchemaRepository) {
    def listSchemas(): Iterable[Schema] = {
      schemaRepository.findAll()
    }.asScala
    def getSchema(schemaId: String): Schema = {
      schemaRepository.findOne(schemaId)
    }
    def createSchema(schema: Schema): String = {
      schemaRepository.save(schema)
      schema.schemaId
    }
  }
