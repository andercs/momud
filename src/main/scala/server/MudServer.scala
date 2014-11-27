package server

import java.net.ServerSocket

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Inbox
import akka.actor.Props
import users.ClientConnection
import users.User
import world.Gaia

case class StartServer()
case class UserLogon(username: String, user: ActorRef)
case class UserLogoff(username: String)

class MudServer extends Actor {
  private var onlineUsers: Map[String, ActorRef] = Map[String, ActorRef]()
  
  def receive = {
    case StartServer => {
      context.actorOf(Props(new Actor {
        def receive = {
          case StartServer => {
            startServer
          }
        }
      }), "serverDaemon") ! StartServer
    }
    case UserLogon(username, user) => attemptLogon(username, user)
    case UserLogoff(username) => attemptLogoff(username)
  }

  private def startServer = {
    val serverPort = new ServerSocket(8080)

    while (true) {
      println("Listening for connections...")
      val clientConnection = serverPort.accept
      val user = context.actorOf(Props(new User))
      println("Connection made, creating user...")
      user ! ClientConnection(clientConnection)
    }
  }
  
  private def attemptLogon(username: String, user: ActorRef) = {
    if (onlineUsers.contains(username.toUpperCase())) {
      sender ! false
    } else {
      onlineUsers = onlineUsers + ((username.toUpperCase(), user))
      sender ! true
    }
  }
  
  private def attemptLogoff(username: String) = {
    onlineUsers = onlineUsers - username.toUpperCase()
  }
}

object MudServer extends App {
  val system = ActorSystem("MorgantownMUD")
  val server = system.actorOf(Props(classOf[MudServer]), "server")
  val gaia = system.actorOf(Props(classOf[Gaia]), "gaia")
  println("Server starting...")
  server ! StartServer
  println("Building world...")  
  val inbox = Inbox.create(system)
  inbox.send(gaia, Gaia.BuildWorld)
}