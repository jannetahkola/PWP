package fi.jannetahkola.palikka.game.service.factory;

import java.net.Socket;

public class SocketFactory {
    public Socket newSocket() {
        return new Socket();
    }
}
