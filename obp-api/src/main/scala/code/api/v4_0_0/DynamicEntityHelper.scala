package code.api.v4_0_0

import code.api.util.APIUtil.{Catalogs, ResourceDoc, authenticationRequiredMessage, emptyObjectJson, generateUUID, notCore, notOBWG, notPSD2}
import code.api.util.ApiTag.{ResourceDocTag, apiTagApi, apiTagNewStyle}
import code.api.util.ErrorMessages.{InvalidJsonFormat, UnknownError, UserHasMissingRoles, UserNotLoggedIn}
import code.api.util.{APIUtil, ApiRole, ApiTag, NewStyle}
import code.api.util.ApiRole.getOrCreateDynamicApiRole
import com.openbankproject.commons.model.enums.DynamicEntityFieldType
import com.openbankproject.commons.util.ApiVersion
import net.liftweb.json.JsonDSL._
import net.liftweb.json._
import net.liftweb.util.StringHelpers
import org.apache.commons.lang3.StringUtils
import org.atteo.evo.inflector.English

import scala.collection.immutable.{List, Nil}
import scala.collection.mutable.ArrayBuffer


object EntityName {

  def unapply(entityName: String): Option[String] = MockerConnector.definitionsMap.keySet.find(entityName ==)

  def unapply(url: List[String]): Option[(String, String)] = url match {
    case entityName :: id :: Nil => MockerConnector.definitionsMap.keySet.find(entityName ==).map((_, id))
    case _ => None
  }

}

object MockerConnector {

  def definitionsMap = NewStyle.function.getDynamicEntities().map(it => (it.entityName, DynamicEntityInfo(it.metadataJson, it.entityName))).toMap

  def doc = {
    val docs: Seq[ResourceDoc] = definitionsMap.values.flatMap(createDocs).toSeq
    collection.mutable.ArrayBuffer(docs:_*)
  }

  // TODO the requestBody and responseBody is not correct ref type
  def createDocs(dynamicEntityInfo: DynamicEntityInfo) = {
    val entityName = dynamicEntityInfo.entityName
    // e.g: "someMultiple-part_Name" -> ["Some", "Multiple", "Part", "Name"]
    val capitalizedNameParts = entityName.split("(?<=[a-z])(?=[A-Z])|-|_").map(_.capitalize).filterNot(_.trim.isEmpty)
    val singularName = capitalizedNameParts.mkString(" ")
    val pluralName = English.plural(singularName)

    val idNameInUrl = StringHelpers.snakify(dynamicEntityInfo.idName).toUpperCase()
    val listName = dynamicEntityInfo.listName

    val endPoint = APIMethods400.Implementations4_0_0.genericEndpoint
    val implementedInApiVersion = ApiVersion.v4_0_0
    val resourceDocs = ArrayBuffer[ResourceDoc]()
    val apiTag: ResourceDocTag = {
      val addPrefix = APIUtil.getPropsAsBoolValue("dynamic_entities_have_prefix", true)

      def existsSameStaticEntity: Boolean = ApiTag.allDisplayTagNames
            .exists(it => it.equalsIgnoreCase(singularName) || it.equalsIgnoreCase(entityName))

      if((addPrefix && !singularName.startsWith("_")) || existsSameStaticEntity) {
        ApiTag("_" + singularName)
      } else {
        ApiTag(' ' + singularName)
      }
    };
    val connectorMethods = Some(List(s"""dynamicEntityProcess: parameters contains {"key": "entityName", "value": "$entityName"}"""))
    resourceDocs += ResourceDoc(
      endPoint,
      implementedInApiVersion,
      s"getAll$entityName",
      "GET",
      s"/$entityName",
      s"Get All $pluralName",
      s"""Get All $pluralName.
         |${dynamicEntityInfo.description}
         |
         |${dynamicEntityInfo.fieldsDescription}
         |
         |Can do filter on the fields
         |e.g: /${entityName}?name=James%20Brown&number=123.456&number=11.11
         |Will do filter by this rule: name == "James Brown" && (number==123.456 || number=11.11)
         |""".stripMargin,
      emptyObjectJson,
      dynamicEntityInfo.getExampleList,
      List(
        UserNotLoggedIn,
        UserHasMissingRoles,
        UnknownError
      ),
      Catalogs(notCore, notPSD2, notOBWG),
      List(apiTag, apiTagApi, apiTagNewStyle),
      Some(List(dynamicEntityInfo.canGetRole)),
      connectorMethods = connectorMethods
    )
    resourceDocs += ResourceDoc(
      endPoint,
      implementedInApiVersion,
      s"getSingle$entityName",
      "GET",
      s"/$entityName/$idNameInUrl",
      s"Get one $singularName",
      s"""Get one $singularName by id.
         |${dynamicEntityInfo.description}
         |
         |${dynamicEntityInfo.fieldsDescription}
         |""".stripMargin,
      emptyObjectJson,
      dynamicEntityInfo.getSingleExample,
      List(
        UserNotLoggedIn,
        UserHasMissingRoles,
        UnknownError
      ),
      Catalogs(notCore, notPSD2, notOBWG),
      List(apiTag, apiTagApi, apiTagNewStyle),
      Some(List(dynamicEntityInfo.canGetRole)),
      connectorMethods = connectorMethods
    )

    resourceDocs += ResourceDoc(
      endPoint,
      implementedInApiVersion,
      s"create$entityName",
      "POST",
      s"/$entityName",
      s"Create one $singularName",
      s"""Create one $singularName.
         |${dynamicEntityInfo.description}
         |
         |${dynamicEntityInfo.fieldsDescription}
         |
         |${authenticationRequiredMessage(true)}
         |
         |""",
      dynamicEntityInfo.getSingleExampleWithoutId,
      dynamicEntityInfo.getSingleExample,
      List(
        UserNotLoggedIn,
        UserHasMissingRoles,
        InvalidJsonFormat,
        UnknownError
      ),
      Catalogs(notCore, notPSD2, notOBWG),
      List(apiTag, apiTagApi, apiTagNewStyle),
      Some(List(dynamicEntityInfo.canCreateRole)),
      connectorMethods = connectorMethods
      )

    resourceDocs += ResourceDoc(
      endPoint,
      implementedInApiVersion,
      s"update$entityName",
      "PUT",
      s"/$entityName/$idNameInUrl",
      s"Update one $singularName",
      s"""Update one $singularName.
         |${dynamicEntityInfo.description}
         |
         |${dynamicEntityInfo.fieldsDescription}
         |
         |${authenticationRequiredMessage(true)}
         |
         |""",
      dynamicEntityInfo.getSingleExampleWithoutId,
      dynamicEntityInfo.getSingleExample,
      List(
        UserNotLoggedIn,
        UserHasMissingRoles,
        InvalidJsonFormat,
        UnknownError
      ),
      Catalogs(notCore, notPSD2, notOBWG),
      List(apiTag, apiTagApi, apiTagNewStyle),
      Some(List(dynamicEntityInfo.canUpdateRole)),
      connectorMethods = connectorMethods
    )

    resourceDocs += ResourceDoc(
      endPoint,
      implementedInApiVersion,
      s"delete$entityName",
      "DELETE",
      s"/$entityName/$idNameInUrl",
      s"Delete one $singularName",
      s"""Delete one $singularName
         |
         |
         |${authenticationRequiredMessage(true)}
         |
         |""",
      dynamicEntityInfo.getSingleExampleWithoutId,
      dynamicEntityInfo.getSingleExample,
      List(
        UserNotLoggedIn,
        UserHasMissingRoles,
        InvalidJsonFormat,
        UnknownError
      ),
      Catalogs(notCore, notPSD2, notOBWG),
      List(apiTag, apiTagApi, apiTagNewStyle),
      Some(List(dynamicEntityInfo.canDeleteRole)),
      connectorMethods = connectorMethods
    )

    resourceDocs
  }

}
case class DynamicEntityInfo(definition: String, entityName: String) {

  import net.liftweb.json

  val subEntities: List[DynamicEntityInfo] = Nil

  val idName = StringUtils.uncapitalize(entityName) + "Id"

  val listName = StringHelpers.snakify(English.plural(entityName))

  val jsonTypeMap: Map[String, Class[_]] = DynamicEntityFieldType.nameToValue.mapValues(_.jValueType)

  val definitionJson = json.parse(definition).asInstanceOf[JObject]
  val entity = (definitionJson \ entityName).asInstanceOf[JObject]

  val description = entity \ "description" match {
    case JString(s) if StringUtils.isNotBlank(s) =>
      s"""
        |**Entity Description:**
        |$s
        |""".stripMargin
    case _ => ""
  }

  val fieldsDescription = {
    val descriptions = (entity \ "properties")
      .asInstanceOf[JObject]
      .obj
      .filter(field =>
        field.value \ "description" match {
          case JString(s) if StringUtils.isNotBlank(s) => true
          case _ => false
        }
      )
      if(descriptions.nonEmpty) {
        descriptions
          .map(field => s"""* ${field.name}: ${(field.value \ "description").asInstanceOf[JString].s}""")
          .mkString("**Properties Description:** \n", "\n", "")
      } else {
        ""
      }
  }

  def toResponse(result: JObject, id: Option[String]): JObject = {

    val fieldNameToTypeName: Map[String, String] = (entity \ "properties")
      .asInstanceOf[JObject]
      .obj
      .map(field => (field.name, (field.value \ "type").asInstanceOf[JString].s))
      .toMap

    val fieldNameToType: Map[String, Class[_]] = fieldNameToTypeName
      .mapValues(jsonTypeMap(_))

    val fields = result.obj.filter(it => fieldNameToType.keySet.contains(it.name))

    (id, fields.exists(_.name == idName)) match {
      case (Some(idValue), false) => JObject(JField(idName, JString(idValue)) :: fields)
      case _ => JObject(fields)
    }
  }

  def getSingleExampleWithoutId: JObject = {
    val fields = (entity \ "properties").asInstanceOf[JObject].obj

    def extractExample(typeAndExample: JValue): JValue = {
      val example = typeAndExample \ "example"
      (example, (typeAndExample \ "type")) match {
        case (JString(s), JString("boolean")) => JBool(s.toLowerCase().toBoolean)
        case (JString(s), JString("integer")) => JInt(s.toLong)
        case (JString(s), JString("number")) => JDouble(s.toDouble)
        case _ => example
      }
    }
    val exampleFields = fields.map(field => JField(field.name, extractExample(field.value)))
    JObject(exampleFields)
  }
  def getSingleExample: JObject = JObject(JField(idName, JString(generateUUID())) :: getSingleExampleWithoutId.obj)

  def getExampleList: JObject =   listName -> JArray(List(getSingleExample))

  val canCreateRole: ApiRole = DynamicEntityInfo.canCreateRole(entityName)
  val canUpdateRole: ApiRole = DynamicEntityInfo.canUpdateRole(entityName)
  val canGetRole: ApiRole = DynamicEntityInfo.canGetRole(entityName)
  val canDeleteRole: ApiRole = DynamicEntityInfo.canDeleteRole(entityName)
}

object DynamicEntityInfo {
  def canCreateRole(entityName: String): ApiRole = getOrCreateDynamicApiRole("CanCreateDynamic" + entityName)
  def canUpdateRole(entityName: String): ApiRole = getOrCreateDynamicApiRole("CanUpdateDynamic" + entityName)
  def canGetRole(entityName: String): ApiRole = getOrCreateDynamicApiRole("CanGetDynamic" + entityName)
  def canDeleteRole(entityName: String): ApiRole = getOrCreateDynamicApiRole("CanDeleteDynamic" + entityName)

  def roleNames(entityName: String): List[String] = List(
      canCreateRole(entityName), canUpdateRole(entityName),
      canGetRole(entityName), canDeleteRole(entityName)
    ).map(_.toString())
}