package domain

import actors.DigraphActor
import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport

class MainModule extends AbstractModule with AkkaGuiceSupport {

  override def configure(): Unit = {
    bindActor[DigraphActor]("DigraphActor")
  }

}
