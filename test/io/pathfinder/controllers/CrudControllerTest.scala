package io.pathfinder.controllers

import io.pathfinder.websockets.ModelTypes
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.contentAsJson
import io.pathfinder.data.{Resource, CrudDao}
import org.mockito.Mockito.when
import org.mockito.Matchers.{anyLong,isA}

object CrudControllerTest extends MockitoSugar {

    case class TestModel(var id:Long,var name:String,var value:String)

    implicit val format = Json.format[TestModel]
    implicit val updateReads = Json.reads[TestResource]

    case class TestResource(
        name:  Option[String],
        value: Option[String]
    ) extends Resource[TestModel] {
        override def update(m: TestModel): Option[TestModel] = {
            name.foreach(m.name = _)
            value.foreach(m.value = _)
            Some(m)
        }

        override def create: Option[TestModel] = for{
            n <- name
            v <- value
        } yield TestModel(0,n,v)
    }

    val mockDao = mock[CrudDao[Long,TestModel]]
    object TestController extends CrudController[Long,TestModel](mockDao) {
        override val model = ModelTypes.Commodity
    }
}

class CrudControllerTest extends ControllerTest with MockitoSugar {
    import CrudControllerTest.{mockDao,TestController,TestModel,TestResource}

    "CrudController#getAll()" should {
        when(mockDao.readAll) thenReturn Seq(TestModel(1,"ONE","1"),TestModel(2,"TWO","2"),TestModel(3,"THREE","3"))
        val result = TestController.getAll.apply(FakeRequest())

        "respond with status 200 when successful" in {
             await(result).header.status mustBe 200
        }
        
        "respond with all items in the table as JSON" in {
            contentAsJson(result).toString mustBe
                    """[{"id":1,"name":"ONE","value":"1"},{"id":2,"name":"TWO","value":"2"},{"id":3,"name":"THREE","value":"3"}]"""
        }
    }

    "CrudController#get(id: K)" should {

        when(mockDao.read(3)).thenReturn(Some(TestModel(3,"MY NAME!","MY ID IS 3!")))
        val result = TestController.get(3).apply(FakeRequest())

        "respond with status 200 when successful" in {
            await(result).header.status mustBe 200
        }

        "respond with the model as JSON when successful" in {
            contentAsJson(result).toString mustBe """{"id":3,"name":"MY NAME!","value":"MY ID IS 3!"}"""
        }

        "respond with 404 if the specified id does not exist" in {
            when(mockDao.read(1)) thenReturn None
            TestController.get(1).apply(FakeRequest()).value.get.get.header.status mustBe 404
        }
    }

    "CrudController#post()" should {
        var request = FakeRequest("POST","/").withBody(
            Json.parse("""{"value":"THIS IS A NEW RECORD","name":"name"}""")
        )

        when(mockDao.create(isA(classOf[TestResource]))) thenReturn Some(TestModel(3,"name","THIS IS A NEW RECORD"))

        val result = TestController.post().apply(request)

        "respond with status 201 when successful" in {
            await(result).header.status mustBe 201
        }

        "respond with the newly created model when successful" in {
            contentAsJson(result).toString mustBe """{"id":3,"name":"name","value":"THIS IS A NEW RECORD"}"""
        }

        when(mockDao.create(isA(classOf[TestResource]))) thenReturn None

        request = FakeRequest("POST","/").withBody(Json.parse("""{"value":"I HAVE NO NAME"}"""))
        val result2 = TestController.post().apply(request)

        "respond with a status 400 when given an incomplete model" in {
            await(result2).header.status mustBe 400            
        }

        request = FakeRequest("POST","/").withBody(Json.parse("""{"value":"value","name":"name","BADFIELD":"hello"}"""))
        val result3 = TestController.post().apply(request)

        "respond with a status 400 when model contains invalid fields" in {
            await(result3).header.status mustBe 400
        }
    }

    "CrudController#put(id: K)" should {
        var request = FakeRequest("PUT","/").withBody(
            Json.parse("""{"name":"numero dos","value":"VAL"}""")        
        )

        when(mockDao.update(anyLong(),isA(classOf[Resource[TestModel]]))) thenReturn Some(TestModel(2,"numero dos","VAL"))

        val response = TestController.put(2).apply(request)
        "respond with a status 200 when successful" in {
            await(response).header.status mustBe 200
        }

        "respond with the updated model when successful" in {
            contentAsJson(response).toString mustBe """{"id":2,"name":"numero dos","value":"VAL"}"""
        }

        when(mockDao.update(anyLong,isA(classOf[Resource[TestModel]]))) thenReturn None

        val response2 = TestController.put(4).apply(request)
        "respond with status 404 if the specified id is not available" in {
            await(response2).header.status mustBe 404
        }

        request = FakeRequest("PUT","/").withBody(Json.parse("""{"value":"value","name":"name","BADFIELD":"hello"}"""))
        val response3 = TestController.post().apply(request)
        "respond with a status 400 when model contains invalid fields" in {
            await(response3).header.status mustBe 400
        }
    }

    "CrudController#delete(id: K)" should {
        var request = FakeRequest("DELETE","/")
        when(mockDao.delete(5)) thenReturn Some(TestModel(5,"name","data"))
        val response = TestController.delete(5).apply(request)

        "respond with code 200 when successful" in {
            await(response).header.status mustBe 200
        }

        "respond with model as it is right before being deleted" in {
            contentAsJson(response).toString mustBe """{"id":5,"name":"name","value":"data"}"""
        }

        when(mockDao.delete(5)) thenReturn None
        val response2 = TestController.delete(5).apply(request)

        "should return with code 404 if specified id doesn't exit" in {
            await(response2).header.status mustBe 404
        }
    }
}
