/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.excilys.com)
 * Copyright 2012 Gilt Groupe, Inc. (www.gilt.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.action.ws

import com.ning.http.client.websocket.{ WebSocket, WebSocketCloseCodeReasonListener, WebSocketTextListener => AHCWebSocketTextListener }

import akka.actor.ActorRef
import io.gatling.core.util.TimeHelper.nowMillis
import io.gatling.http.ahc.WebSocketTx

class WebSocketListener(tx: WebSocketTx, wsActor: ActorRef, started: Long)
    extends AHCWebSocketTextListener with WebSocketCloseCodeReasonListener {

  private var state: WebSocketListenerState = Opening

  def onOpen(webSocket: WebSocket) {
    state = Open
    wsActor ! OnOpen(tx, webSocket, started, nowMillis)
  }

  def onMessage(message: String) {
    wsActor ! OnMessage(message)
  }

  def onFragment(fragment: String, last: Boolean) {}

  def onClose(webSocket: WebSocket) {}

  def onClose(webSocket: WebSocket, statusCode: Int, reason: String) {
    state match {
      case Open =>
        state = Closed
        val closeMessage = if (statusCode == 1006) OnUnexpectedClose else OnClose
        wsActor ! closeMessage

      case _ => // discard
    }
  }

  def onError(t: Throwable) {
    state match {
      case Opening =>
        wsActor ! OnFailedOpen(tx, t.getMessage, started, nowMillis)

      case Open =>
        wsActor ! OnError(t)

      case Closed => // discard
    }
  }
}

private sealed trait WebSocketListenerState

private case object Opening extends WebSocketListenerState

private case object Open extends WebSocketListenerState

private case object Closed extends WebSocketListenerState
