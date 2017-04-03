package io.pioneerlabs.microtest.pact.model.v3.messaging

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.Canonical
import io.pioneerlabs.microtest.pact.model.HttpPart
import io.pioneerlabs.microtest.pact.model.Interaction
import io.pioneerlabs.microtest.pact.model.OptionalBody
import io.pioneerlabs.microtest.pact.model.Response
import org.apache.commons.lang.StringUtils

/**
 * Message in a Message Pact
 */
@Canonical
class Message implements Interaction {
  private static final String JSON = 'application/json'

  String description
  String providerState
  OptionalBody contents = OptionalBody.missing()
  Map<String, Map<String, Object>> matchingRules = [:]
  Map<String, String> metaData = [:]

  byte[] contentsAsBytes() {
    if (contents.present) {
      contents.value.toString().bytes
    } else {
      []
    }
  }

  String getContentType() {
    metaData.contentType ?: JSON
  }

  Map toMap() {
    def map = [
      description: description,
      metaData: metaData
    ]
    if (!contents.missing) {
      map.contents = formatContents()
    }
    if (providerState) {
      map.providerState = providerState
    }
    if (matchingRules) {
      map.matchingRules = matchingRules
    }
    map
  }

  def formatContents() {
    if (contents.present) {
      switch (contentType) {
        case JSON: return new JsonSlurper().parseText(contents.value.toString())
        case 'application/octet-stream': return contentsAsBytes().encodeBase64().toString()
        default: return contents.value.toString()
      }
    } else {
      ''
    }
  }

  Message fromMap(Map map) {
    description = map.description ?: ''
    providerState = map.providerState
    if (map.containsKey('contents')) {
      if (map.contents == null) {
        contents = OptionalBody.nullBody()
      } else if (map.contents instanceof String && map.contents.empty) {
        contents = OptionalBody.empty()
      } else {
        contents = OptionalBody.body(JsonOutput.toJson(map.contents))
      }
    }
    matchingRules = map.matchingRules ?: [:]
    metaData = map.metaData ?: [:]
    this
  }

  HttpPart asPactRequest() {
    new Response(200, ['Content-Type': contentType], contents, matchingRules)
  }

  @Override
  boolean conflictsWith(Interaction other) {
//    TODO: Need to match the bodies
//    if (other instanceof Message) {
//      description == other.description &&
//        providerState == other.providerState &&
//        formatContents() != other.formatContents()
//    } else {
//      false
//    }
    !(other instanceof Message)
  }

  @Override
  String uniqueKey() {
    "${StringUtils.defaultIfEmpty(providerState, 'None')}_$description"
  }
}
