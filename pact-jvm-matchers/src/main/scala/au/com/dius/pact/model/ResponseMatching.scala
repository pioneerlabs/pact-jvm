package au.com.dius.pact.model

import io.pioneerlabs.microtest.pact.model.Response

sealed trait ResponseMatch
case object FullResponseMatch extends ResponseMatch
case class ResponseMismatch(mismatches: Seq[ResponsePartMismatch]) extends ResponseMatch

object ResponseMatching extends ResponseMatching(DiffConfig(allowUnexpectedKeys = true, structural = false))

class ResponseMatching(val providerDiffConfig: DiffConfig) {
  import au.com.dius.pact.model.Matching._

  def matchRules(expected: Response, actual: Response): ResponseMatch = {
    val mismatches = responseMismatches(expected, actual)
    if (mismatches.isEmpty) FullResponseMatch
    else ResponseMismatch(mismatches)
  }
  
  def responseMismatches(expected: Response, actual: Response): Seq[ResponsePartMismatch] = {
    (matchStatus(expected.getStatus, actual.getStatus)
      ++ matchHeaders(expected, actual)
      ++ matchBody(expected, actual, providerDiffConfig)).toSeq
  }
}
