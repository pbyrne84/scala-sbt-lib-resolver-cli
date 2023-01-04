package com.pbyrne84.github.scala.github.mavensearchcli.maven.client

import zio.Scope
import zio.test._

import java.net.URLEncoder

object SolrSearchUrlSpec extends ZIOSpecDefault {
  private val serverHost = "http://localhost:1234"

  override def spec: Spec[TestEnvironment with Scope, Any] = suite("asString")(
    test("should encode the url when the page is 1") {
      val orgName = "com.me"
      val pageSize = 14
      val rowOffset = 0
      val expected =
        s"http://localhost:1234/solrsearch/select?q=g:${URLEncoder.encode(orgName, "UTF-8")}" +
          s"&core=gav&start=$rowOffset&rows=$pageSize"

      assertTrue(
        SolrSearchUrl(serverHost = serverHost, orgName = orgName, pageSize = 14, pageNumber = 1).fullUrl == expected
      )
    },
    test("should encode the url when the page is 2") {
      val orgName = "com.me"
      val pageSize = 14
      val rowOffset = 14
      val expected =
        s"http://localhost:1234/solrsearch/select?q=g:${URLEncoder.encode(orgName, "UTF-8")}" +
          s"&core=gav&start=$rowOffset&rows=$pageSize"

      val actual = SolrSearchUrl(serverHost = serverHost, orgName = orgName, pageSize = 14, pageNumber = 2).fullUrl

      println(s"actual = $actual")

      assertTrue(
        actual == expected
      )
    }
  )
}
