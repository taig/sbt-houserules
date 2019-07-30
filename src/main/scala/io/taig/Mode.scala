package io.taig

sealed abstract class Mode extends Product with Serializable

object Mode {
  case object Strict extends Mode
  case object Tolerant extends Mode

  val Default: Mode = Tolerant

  def parse(value: String): Option[Mode] =
    PartialFunction.condOpt(value) {
      case "tolerant" => Tolerant
      case "strict"   => Strict
    }
}
