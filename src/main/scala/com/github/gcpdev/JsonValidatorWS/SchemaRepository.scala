package com.github.gcpdev.JsonValidatorWS

import org.springframework.data.repository.CrudRepository
import java.lang.Long

  trait SchemaRepository extends CrudRepository[Schema, String] {
//    def findSchemaById(id: Long): Schema
  }

