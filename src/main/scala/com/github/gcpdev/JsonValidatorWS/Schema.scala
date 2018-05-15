package com.github.gcpdev.JsonValidatorWS


import javax.persistence._


import scala.beans.BeanProperty


@Entity
class Schema extends Serializable {
  //@Id
  //@GeneratedValue(strategy = GenerationType.IDENTITY)
  //@BeanProperty
  //var id: Long
  @Id
  @BeanProperty
  var schemaId: String = _
  @BeanProperty
  @Column(name = "schema", columnDefinition="LONGVARCHAR")
  var schema: String = _
}