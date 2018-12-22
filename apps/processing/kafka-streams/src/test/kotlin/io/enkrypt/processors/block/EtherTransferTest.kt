package io.enkrypt.processors.block

import io.enkrypt.avro.capture.BlockRecord
import io.enkrypt.common.extensions.*
import io.enkrypt.kafka.streams.models.ChainEvent
import io.enkrypt.kafka.streams.models.ChainEvent.Companion.fungibleTransfer
import io.enkrypt.kafka.streams.models.StaticAddresses.EtherZero
import io.enkrypt.kafka.streams.processors.block.ChainEvents
import io.enkrypt.util.Blockchains
import io.enkrypt.util.Blockchains.Coinbase
import io.enkrypt.util.Blockchains.Users.Alice
import io.enkrypt.util.Blockchains.Users.Bob
import io.enkrypt.util.Blockchains.Users.Terence
import io.enkrypt.util.StandaloneBlockchain
import io.kotlintest.matchers.collections.shouldContainExactly
import io.kotlintest.shouldBe
import io.kotlintest.specs.BehaviorSpec

class EtherTransferTest : BehaviorSpec() {

  private val premineBalances = mapOf(
    Bob.address.data20() to 20.ether(),
    Alice.address.data20() to 50.ether(),
    Terence.address.data20() to 100.ether()
  )

  val bcConfig = StandaloneBlockchain.Config(
    gasLimit = 21000,
    gasPrice = 1.gwei().toLong(),
    premineBalances = premineBalances,
    coinbase = Blockchains.Coinbase.address.data20()!!
  )

  val bc = StandaloneBlockchain(bcConfig)

  init {

    given("a block with a series of valid ether transfers") {

      bc.sendEther(Bob, Alice, 50.gwei())
      bc.sendEther(Alice, Terence, 25.gwei())
      bc.sendEther(Terence, Bob, 125.gwei())

      val block = bc.createBlock()

      `when`("we convert the block") {

        val chainEvents = ChainEvents.forBlock(block)

        then("there should be 7 chain events") {
          chainEvents.size shouldBe 7
        }

        then("there should be a fungible ether transfer for the coinbase") {
          chainEvents.first() shouldBe fungibleTransfer(EtherZero, Coinbase.address.data20()!!, 3000063000.gwei().byteBuffer()!!)
        }

        then("there should be an ether transfer between bob and alice") {
          chainEvents[1] shouldBe fungibleTransfer(Bob.address.data20()!!, EtherZero, 21000.gwei().byteBuffer()!!)                // tx fee
          chainEvents[2] shouldBe fungibleTransfer(Bob.address.data20()!!, Alice.address.data20()!!, 50.gwei().byteBuffer()!!)
        }

        then("there should be an ether transfer between alice and terence") {
          chainEvents[3] shouldBe fungibleTransfer(Alice.address.data20()!!, EtherZero, 21000.gwei().byteBuffer()!!)                // tx fee
          chainEvents[4] shouldBe fungibleTransfer(Alice.address.data20()!!, Terence.address.data20()!!, 25.gwei().byteBuffer()!!)
        }

        then("there should be an ether transfer between terence and bob") {
          chainEvents[5] shouldBe fungibleTransfer(Terence.address.data20()!!, EtherZero, 21000.gwei().byteBuffer()!!)                // tx fee
          chainEvents[6] shouldBe fungibleTransfer(Terence.address.data20()!!, Bob.address.data20()!!, 125.gwei().byteBuffer()!!)
        }
      }

      `when`("we reverse the block") {

        val reverseBlockRecord = BlockRecord.newBuilder(block)
          .setReverse(true)
          .build()

        val reversedChainEvents = ChainEvents.forBlock(reverseBlockRecord)

        then("there should be 7 chain events") {
          reversedChainEvents.size shouldBe 7
        }

        then("there should be a reversed fungible ether transfer for each originating transfer") {

          reversedChainEvents shouldContainExactly listOf(
            fungibleTransfer(EtherZero, Coinbase.address.data20()!!, 3000063000.gwei().byteBuffer()!!, true),
            fungibleTransfer(Bob.address.data20()!!, EtherZero, 21000.gwei().byteBuffer()!!, true),
            fungibleTransfer(Bob.address.data20()!!, Alice.address.data20()!!, 50.gwei().byteBuffer()!!, true),
            fungibleTransfer(Alice.address.data20()!!, EtherZero, 21000.gwei().byteBuffer()!!, true),
            fungibleTransfer(Alice.address.data20()!!, Terence.address.data20()!!, 25.gwei().byteBuffer()!!, true),
            fungibleTransfer(Terence.address.data20()!!, EtherZero, 21000.gwei().byteBuffer()!!, true),
            fungibleTransfer(Terence.address.data20()!!, Bob.address.data20()!!, 125.gwei().byteBuffer()!!, true)
          ).asReversed()

        }
      }
    }

    given("a block with some invalid ether transfers") {

      bc.sendEther(Bob, Alice, 100.ether())
      bc.sendEther(Alice, Terence, 56.gwei())
      bc.sendEther(Terence, Bob, 200.ether())

      val block = bc.createBlock()

      `when`("we convert the block") {

        val chainEvents = ChainEvents.forBlock(block)

        then("there should be 3 chain events") {
          chainEvents.size shouldBe 3
        }

        then("there should be a fungible ether transfer for the coinbase") {
          chainEvents.first() shouldBe fungibleTransfer(EtherZero, Coinbase.address.data20()!!, 3000021000.gwei().byteBuffer()!!)
        }

        then("there should be a single transfer from alice to terence") {
          chainEvents[1] shouldBe fungibleTransfer(Alice.address.data20()!!, EtherZero, 21000.gwei().byteBuffer()!!)                // tx fee
          chainEvents[2] shouldBe fungibleTransfer(Alice.address.data20()!!, Terence.address.data20()!!, 56.gwei().byteBuffer()!!)
        }
      }
    }
  }
}
