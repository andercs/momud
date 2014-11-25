package world

import akka.actor._
import scala.slick.driver.MySQLDriver.simple._
import scala.slick.jdbc.StaticQuery.interpolation
import scala.collection.immutable.Map
import scala.concurrent.duration._
import scala.slick.jdbc.StaticQuery.staticQueryToInvoker
import rooms._

object Gaia {
	case class BuildWorld()
}

class Gaia extends Actor{
	import Room._
	import Gaia._
	val db = Database.forURL("jdbc:mysql://localhost:3306/mud", driver="com.mysql.jdbc.Driver", user="root", password="root")
  val rooms: TableQuery[Rooms] = TableQuery[Rooms]
  val exits: TableQuery[Exits] = TableQuery[Exits]
  var world: Map[Int,(String,ActorRef)] = Map();
	
	def receive = {
		case BuildWorld => buildWorld; sender ! world.toString
	}
	
	def buildWorld = {
		db.withSession { implicit session => 
			createRooms
			setExits
			def createRooms = {
				rooms foreach { case (id, name, desc) =>
		  		val room: ActorRef = context.actorOf(Room.props(id, name, desc))
		  		val mapping: (Int,(String,ActorRef)) = (id,(name,room))
		  		world = world + mapping
				}
			}
			def setExits = {
				world foreach { case(id, room) =>
	  			val exitQuery = sql"SELECT exits.dir, exits.to FROM exits JOIN rooms ON rooms.id = exits.from WHERE exits.from = $id".as[(String,Int)]
					var exits: Map[String,(String,ActorRef)] = Map()
					exitQuery foreach { case(dir, dest) =>
	  				val mapping: (String,(String,ActorRef)) = (dir, world(dest))
	  				exits = exits + mapping
	  			}
	  			room._2  ! SetExits(exits)
				}
			}
		}
	}
}
