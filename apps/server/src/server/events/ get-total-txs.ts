import { TotalTxsPayload } from '@app/server/core/payloads'
import { totalTxsPayloadValidator } from '@app/server/core/validation'
import { EthVMServer, SocketEvent, SocketEventValidationResult } from '@app/server/ethvm-server'
import { Events } from '@app/server/core/events'

const getTotalTxsEvent: SocketEvent = {
  id: Events.getTotalTxs, // new_name: get-total-txs

  onValidate: (server: EthVMServer, socket: SocketIO.Socket, payload: any): SocketEventValidationResult => {
    const valid = totalTxsPayloadValidator(payload) as boolean
    return {
      valid,
      errors: [] // TODO: Map properly the error
    }
  },

  onEvent: (server: EthVMServer, socket: SocketIO.Socket, payload: TotalTxsPayload): Promise<number> => server.accountsService.getTotalTxs(payload.address)
}

export default getTotalTxsEvent
