package model.dao

import model.Course
import model.persistence._
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

object Courses extends CachedPersistence[Long, Option[Long], Course] with StrongCacheLike[Long, Option[Long], Course] {
  import ctx._

  /** A real application would provide a dedicated `ExecutionContext` for DAOs */
  implicit val ec: ExecutionContext = global

  override val _findAll: List[Course] =
    run { quote { query[Course] } }

  val queryById: Id[Option[Long]] => Quoted[EntityQuery[Course]] =
    (id: Id[Option[Long]]) =>
      quote { query[Course].filter(_.id == lift(id)) }

  val _deleteById: (Id[Option[Long]]) => Unit =
    (id: Id[Option[Long]]) => {
      run { quote { queryById(id).delete } }
      ()
    }

  val _findById: Id[Option[Long]] => Option[Course] =
    (name: Id[Option[Long]]) =>
      run { quote { queryById(name) } }.headOption

  val _insert: Course => Course =
    (course: Course) => {
      val id: Id[Option[Long]] = run { quote { query[Course].insert(lift(course)) }.returning(_.id) }
      course.copy(id=id)
    }

  val _update: Course => Course =
    (course: Course) => {
      val id: Long = run { queryById(course.id).update(lift(course)) }
      course.copy(id = Id(Some(id)))
    }

  val className: String = "Course"
  val skuBase: String = className.toLowerCase

  override protected val sanitize: (Course) => Course =
    (course: Course) => course

  /** Inserts newCourse into database and augments cache. */
  override def add(newCourse: Course): Course =
    findBySku(newCourse.sku) match {
      case Some(course) => // course already exists, update it and return it
        upsert(course)

      case None =>
        val modifiedCourse: Course = newCourse.id.value match {
          case _: Some[Long] => // todo consolidate with save?
            val sanitizedCourse: Course = sanitize(newCourse)
            update(sanitizedCourse)
            sanitizedCourse

          case None => // new group; insert it and return modified course
            val sanitizedCourse = sanitize(newCourse)
            val inserted: Course = insert(sanitizedCourse)
            cacheSet(inserted.id, inserted)
            inserted
        }
        cacheSet(modifiedCourse.id, modifiedCourse)
        modifiedCourse
    }

  /** @return Option[Course] with specified SKU */
  @inline def findBySku(sku: String): Option[Course] = findAll.find(_.sku == longSku(sku))

  /** Returns all courses in the specified group */
  @inline def findByGroupId(groupId: Id[Option[Long]]): List[Course] = findAll.filter(_.groupId == groupId)


  @inline def longSku(sku: String): String = sku match {
    case name if name.startsWith(s"${ skuBase }_") =>
      name

    case name if name.startsWith(skuBase) =>
      s"${ skuBase }_" + name.substring(skuBase.length)

    case _ =>
      s"${ skuBase }_$sku"
  }
}
