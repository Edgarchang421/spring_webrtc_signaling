package com.example.webrtc;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Timer;
import java.util.TimerTask;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class SocketHandler extends TextWebSocketHandler  {

    // web socket clients list, thread safe
    private static List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

    // user name list, thread safe
    private static List<String> usernames = new CopyOnWriteArrayList<>();

    // username and websocket session map set, thread safe
    private static ConcurrentHashMap<String,WebSocketSession> webSocketClients = new ConcurrentHashMap<String,WebSocketSession>();

    private static final Logger log = LoggerFactory.getLogger(SocketHandler.class);
    
    private static JSONObject j = new JSONObject().put("type", "keepalive");
    private static TextMessage keepAliveMsg = new TextMessage(j.toString());
    
    private String username;

    @Override
	public void handleTextMessage(WebSocketSession session, TextMessage message)
			throws InterruptedException, IOException {

		String payload = message.getPayload();

		JSONObject j = new JSONObject(payload);
        // log.info(j.toString());
        
        String type = j.getString("type");
        String data = j.getString("data");

        JSONObject jsonData = new JSONObject(data);

        switch (type){
            case "new":
                String username = jsonData.getString("username");
                onNewEvent(username, session);
                break;
            case "candidate":
                webrtcNegotiation(type,jsonData);
                break;
            case "offer":
                webrtcNegotiation(type,jsonData);
                break;
            case "answer":
                webrtcNegotiation(type,jsonData);
            break;
        }

	}

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status)throws Exception{
        sessions.remove(session);
        usernames.remove(this.username);
        webSocketClients.remove(this.username);

        notifyAllClient();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 連線建立後，新增到list
        sessions.add(session);
        System.out.println("-------------------------Web Socket Connection opened!-------------------------");

        notifyAllClient();

        log.info("afterConnectionEstablished notifyAllClient finished");

        // keep alive
        Timer t = new Timer();
        TimerTask task = new TimerTask() {
            public void run()
            {
                try {
                    Thread.sleep(3000);
                    session.sendMessage(keepAliveMsg);
                } catch (InterruptedException | IOException e) {
                    // TODO Auto-generated catch block
                    // e.printStackTrace();
                    log.info("keep alive over");
                }
            }
        };
        t.scheduleAtFixedRate(task, 2000, 1000);
    }

    public void onNewEvent(String username,WebSocketSession session) throws IOException{
        Map<String,WebSocketSession> client = new HashMap<String,WebSocketSession>();

        client.put(username,session);

        if (webSocketClients.containsKey(username)){
            JSONObject j = new JSONObject();
            j.put("type", "error");

            JSONObject data = new JSONObject();
            data.put("message","username already used");

            j.put("date", data.toString());

            TextMessage msg = new TextMessage(j.toString());
            session.sendMessage(msg);
            return;
        }

        webSocketClients.put(username, session);

        usernames.add(username);

        this.username = username;
        
        notifyAllClient();

        log.info("clients map : {}",webSocketClients);
    }

    public static void notifyAllClient() throws IOException{
        JSONObject j = new JSONObject();
        j.put("type","notify");

        JSONObject data = new JSONObject();
        data.put("usernames",usernames);

        j.put("data", data.toString());

        TextMessage msg = new TextMessage(j.toString());

        for (WebSocketSession session : sessions){
            session.sendMessage(msg);
            log.info("update users info for all client");
        }
    }

    public void webrtcNegotiation(String type,JSONObject jsonData){
        String receiver = jsonData.getString("to");
        log.info("type {} to receiver: {}",type,receiver);

        JSONObject j = new JSONObject();
        j.put("type", type);
        j.put("data", jsonData.toString());

        TextMessage msg = new TextMessage(j.toString());

        WebSocketSession receiverSession =  webSocketClients.get(receiver);

        try {
            receiverSession.sendMessage(msg);
            // log.info("send to receiver success");
        }catch (IOException e){
            log.info("send failed");
        }
    }

    // @Scheduled(fixedRate = 5000)
	// public void websocketKeepAlive() throws IOException{
        // log.info("username :{} keep alive websocket", this.username);
		// this.webSocketSession.sendMessage(keepAliveMsg);
    //     for (WebSocketSession session :sessions){
    //         session.sendMessage(keepAliveMsg);
    //     };
	// }
}