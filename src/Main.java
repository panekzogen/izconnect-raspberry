import java.io.IOException;

public class Main {
    private static BoardNetworkService networkService = new BoardNetworkService();
    private static BoardServiceImpl boardService = new BoardServiceImpl();

    public static void main(String[] args) throws IOException {
        networkService.doConnect();
        networkService.registerInterface(boardService);
        networkService.announce();

        System.in.read();

        networkService.unregisterInterface(boardService);
        networkService.doDisconnect();
    }
}
