package dao

import java.sql.Timestamp

import com.google.inject.{Singleton, Inject}
import model.{ProductLicense, Ping, Product}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.driver.JdbcProfile

import scala.concurrent.Future

/**
  * Created by wyozi on 8.2.2016.
  */
@Singleton
class PingsDAO @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)
  extends HasDatabaseConfigProvider[JdbcProfile] {

  import driver.api._

  private[dao] val Pings = TableQuery[PingsTable]

  /**
    * Inserts given ping to Pings table and returns id.
    */
  def insert(product: String, license: String, user: String, remoteAddress: String, responseId: Option[Int]): Future[Int] =
    db.run((Pings.map(c => (c.product, c.license, c.userName, c.ip, c.responseId)) returning Pings.map(_.id)) += (product, license, user, remoteAddress, responseId))

  /**
    * Find latest pings sorted by ID descendingly.
    */
  def findRecent(limit: Int): Future[Seq[Ping]] =
    db.run(Pings.sortBy(_.id.desc).take(limit).result)

  /**
    * Find recent pings for given product.
    *
    * @param prod
    * @param limit
    * @param ignoredLicense license to ignore from output
    */
  def findRecentForProduct(prod: Product, limit: Int, ignoredLicense: Option[String] = None): Future[Seq[Ping]] =
    db.run(
      Pings
        .filter(_.product === prod.shortName)
        .filterNot(pi => ignoredLicense.map(pi.license === _).getOrElse(false:Rep[Boolean]))
        .sortBy(_.id.desc)
        .take(limit)
        .result
    )

  /**
    * Find recent pings by given ip.
    */
  def findRecentByIp(ip: String, limit: Int): Future[Seq[Ping]] =
    db.run(Pings.filter(_.ip === ip).sortBy(_.id.desc).take(limit).result)

  /**
    * Find recent pings by given user.
    */
  def findRecentByUser(user: String, limit: Int): Future[Seq[Ping]] =
    db.run(Pings.filter(_.userName === user).sortBy(_.id.desc).take(limit).result)

  /**
    * Find recent pings with given response id.
    */
  def findRecentWithResponse(resp: Option[Int], limit: Int): Future[Seq[Ping]] =
    db.run(Pings.filter(_.responseId === resp).sortBy(_.id.desc).take(limit).result)

  /**
    * Find count of distinct licenses for given product.
    */
  def findLicenseCount(prod: Product): Future[Int] =
    db.run(Pings.filter(_.product === prod.shortName).map(_.license).countDistinct.result)

  /**
    * Find amount of pings for given product.
    */
  def findPingCount(prod: Product): Future[Int] =
    db.run(Pings.filter(_.product === prod.shortName).length.result)

  /**
    * Find recent pings with given license.
    * NOTE: Queries for given license in '''all''' pings no matter the product. If you want to filter by product as well,
    * use findRecentWithProdLicense.
    */
  def findRecentWithLicense(license: model.License, limit: Int): Future[Seq[Ping]] =
    db.run(Pings.filter(_.license === license).sortBy(_.date.desc).take(limit).result)

  /**
    * Find recent pings with given product and license.
    */
  def findRecentWithProdLicense(prodLicense: ProductLicense, limit: Int): Future[Seq[Ping]] =
    db.run(
      Pings.filter(p => p.product === prodLicense.prod.shortName && p.license === prodLicense.license)
        .sortBy(_.date.desc).take(limit).result
    )

  private[dao] class PingsTable(tag: Tag) extends Table[Ping](tag, "pings") {
    def id = column[Int]("id", O.AutoInc)

    def product = column[String]("product", O.SqlType("VARCHAR(255)"))
    def license = column[String]("license", O.SqlType("VARCHAR(255)"))
    def userName = column[String]("user_name", O.SqlType("VARCHAR(64)"))
    def date = column[Timestamp]("date")
    def responseId = column[Option[Int]]("response_id")

    def ip = column[String]("ip", O.SqlType("VARCHAR(16)"))

    override def * = (id, date, product, license, userName, ip, responseId) <> (Ping.tupled, Ping.unapply _)
    //def response = foreignKey("RESPONSE_FK", responseId, )
  }
}
