package com.pbyrne84.github.scala.github.mavensearchcli.commandline

import com.pbyrne84.github.scala.github.mavensearchcli.config._
import com.pbyrne84.github.scala.github.mavensearchcli.error.MissingHotListException
import com.pbyrne84.github.scala.github.mavensearchcli.shared.BaseSpec
import com.pbyrne84.github.scala.github.mavensearchcli.shared.BaseSpec.SharedDeps
import zio.Scope
import zio.test._

object LookupTypeSpec extends BaseSpec {

  override def spec: Spec[SharedDeps with TestEnvironment with Scope, Any] =
    suite(getClass.getSimpleName)(
      suite("getOrgConfigsForLookup should")(
        test("remap selected hotList default") {

          val customDefault = HotListItemConfig(
            name = "customDefault",
            refs = List("group2", "group3")
          )

          val group2Config = GroupConfig("group2", "", List(ModuleConfig("mc2"), ModuleConfig("mc3")))
          val group3Config = GroupConfig("group3", "", List(ModuleConfig("mc4")))

          val actual = DefaultHotListLookupType.getOrgConfigsForLookup(
            SearchConfig(
              defaults = ConfigDefaults(maybeHotList = Some("customDefault")),
              maximumPagesToPaginate = 1,
              hotLists = List(
                HotListItemConfig("list1", List.empty),
                customDefault,
                HotListItemConfig("list2", List.empty)
              ),
              groups = List(
                GroupConfig("group1", "", List(ModuleConfig("mc1"))),
                group2Config,
                group3Config,
                GroupConfig("group4", "", List(ModuleConfig("mc5")))
              )
            )
          )

          assertTrue(actual == Right(List(group2Config, group3Config)))
        },
        test("fail when the selected hotList default has a missing group config") {
          val customDefault = HotListItemConfig(
            name = "customDefault",
            refs = List("group2", "group3")
          )

          val group2Config = GroupConfig("group2", "", List(ModuleConfig("mc2"), ModuleConfig("mc3")))

          val actual = DefaultHotListLookupType.getOrgConfigsForLookup(
            SearchConfig(
              defaults = ConfigDefaults(maybeHotList = Some("customDefault")),
              maximumPagesToPaginate = 1,
              hotLists = List(
                HotListItemConfig("list1", List.empty),
                customDefault,
                HotListItemConfig("list2", List.empty)
              ),
              groups = List(
                GroupConfig("group1", "", List(ModuleConfig("mc1"))),
                group2Config,
                GroupConfig("group4", "", List(ModuleConfig("mc5")))
              )
            )
          )

          assertTrue(actual.left.map(_.getClass) == Left(classOf[MissingHotListException]))
        },
        test("fail when there is no default configured when one is expected") {
          val customDefault = HotListItemConfig(
            name = "customDefault",
            refs = List("group2", "group3")
          )

          val group2Config = GroupConfig("group2", "", List(ModuleConfig("mc2"), ModuleConfig("mc3")))

          val actual = DefaultHotListLookupType.getOrgConfigsForLookup(
            SearchConfig(
              defaults = ConfigDefaults(maybeHotList = None),
              maximumPagesToPaginate = 1,
              hotLists = List(
                HotListItemConfig("list1", List.empty),
                customDefault,
                HotListItemConfig("list2", List.empty)
              ),
              groups = List(
                GroupConfig("group1", "", List(ModuleConfig("mc1"))),
                group2Config,
                GroupConfig("group4", "", List(ModuleConfig("mc5")))
              )
            )
          )

          assertTrue(actual.left.map(_.getClass) == Left(classOf[MissingHotListException]))
        },
        test("remap custom hotList param") {
          val group2Config = GroupConfig("group2", "", List(ModuleConfig("mc2"), ModuleConfig("mc3")))
          val group4Config = GroupConfig("group4", "", List(ModuleConfig("mc5")))
          val actual = CustomHotListLookupType("list2").getOrgConfigsForLookup(
            SearchConfig(
              defaults = ConfigDefaults(maybeHotList = Some("customDefault")),
              maximumPagesToPaginate = 1,
              hotLists = List(
                HotListItemConfig("list1", List.empty),
                HotListItemConfig(
                  name = "customDefault",
                  refs = List("group2", "group3")
                ),
                HotListItemConfig("list2", List("group2", "group4"))
              ),
              groups = List(
                GroupConfig("group1", "", List(ModuleConfig("mc1"))),
                group2Config,
                GroupConfig("group3", "", List(ModuleConfig("mc4"))),
                group4Config
              )
            )
          )

          assertTrue(actual == Right(List(group2Config, group4Config)))
        }
      )
    )
}
