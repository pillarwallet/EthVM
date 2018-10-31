import { TxPayload } from '@app/server/core/payloads'
import { txPayloadValidator } from '@app/server/core/validation'
import { EthVMServer, SocketEvent, SocketEventValidationResult } from '@app/server/ethvm-server'
import { Tx } from 'ethvm-models'
import { Events } from '@app/server/core/events'

const getTxEvent: SocketEvent = {
  id: Events.getTx, // new_name: get-tx

  onValidate: (server: EthVMServer, socket: SocketIO.Socket, payload: any): SocketEventValidationResult => {
    const valid = txPayloadValidator(payload) as boolean
    return {
      valid,
      errors: [] // TODO: Map properly the error
    }
  },

  onEvent: (server: EthVMServer, socket: SocketIO.Socket, payload: TxPayload): Promise<Tx | null> => server.txsService.getTx(payload.hash)
}

export default getTxEvent
