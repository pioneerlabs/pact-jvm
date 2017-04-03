package io.pioneerlabs.microtest.pact.model

import groovy.transform.Canonical

/**
 * Pact provider
 */
@Canonical
class Provider {
  String name

  static Provider fromMap(Map map) {
    new Provider(map?.name ?: 'provider')
  }
}
