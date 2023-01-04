package com.pbyrne84.github.scala.github.mavensearchcli.maven.client

import java.net.URLEncoder

case class SolrSearchUrl(serverHost: String, orgName: String, pageSize: Int, pageNumber: Int) {
  def fullUrl: String = {

    s"${serverHost}$urlWithoutHost"
  }

  val urlWithoutHost: String = {
    val start = (pageNumber - 1) * pageSize

    // &core=gav affects whether we get all versions with a "v" field or just latest version with a "latestVersion" field/
    // Of course we want all versions as latest can be a milestone or something not production ready
    s"/solrsearch/select?q=g:${URLEncoder.encode(orgName, "UTF-8")}&core=gav&start=$start&rows=$pageSize"
  }

}
