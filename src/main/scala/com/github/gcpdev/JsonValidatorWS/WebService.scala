package com.github.gcpdev.JsonValidatorWS

import org.springframework.context.annotation.Configuration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.ComponentScan
import org.springframework.boot.SpringApplication

@Configuration
@EnableAutoConfiguration
@ComponentScan
class WebService

object WebService extends App {
  SpringApplication.run(classOf[WebService])
}