package model.dao

import model.PaymentMechanism
import scala.concurrent.ExecutionContext

/** Define `SelectedCtx` for use with all DAOs; it could provide all implicit Decoder/Encoder/Mappers */
class SelectedCtx extends model.persistence.H2Ctx {
  /** A real application would provide a dedicated `ExecutionContext` for cache and async DAOs instead of using the global default */
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

 // Cannot get this dang thing to work:
 // implicit val paymentMechanismEncoder = new EnumQuillEncoder[PaymentMechanism](ctx)

 // So use the more verbose mechanism:
 implicit val encodePaymentMechanism: MappedEncoding[PaymentMechanism, String] =
     MappedEncoding[PaymentMechanism, String](_.name)

  implicit val decodePaymentMechanism: MappedEncoding[String, PaymentMechanism] =
    MappedEncoding[String, PaymentMechanism](PaymentMechanism.valueOf)


 implicit val encodeListPaymentMechanism: MappedEncoding[List[PaymentMechanism], String] =
   MappedEncoding[List[PaymentMechanism], String](_.map(_.name).mkString(","))

 implicit val decodeListPaymentMechanism: MappedEncoding[String, List[PaymentMechanism]] =
   MappedEncoding[String, List[PaymentMechanism]] { x =>
     val string = x.trim
     if (string.isEmpty) Nil
     else string.split(",").toList.map(PaymentMechanism.valueOf)
   }
}
