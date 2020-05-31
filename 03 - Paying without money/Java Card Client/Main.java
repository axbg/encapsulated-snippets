import java.io.IOException;

import com.sun.javacard.apduio.CadTransportException;

import io.IOManager;
import io.JCardManager;
import io.utils.JCardException;
import ui.MainPanel;

public class Main {

	public static void main(String[] args) {
		JCardManager jcm = null;
		IOManager io = new IOManager();
		MainPanel mainPanel = new MainPanel(io);

		try {
			jcm = new JCardManager();
			mainPanel.setJcm(jcm);
			jcm.loadScripts();
		} catch (IOException | CadTransportException | JCardException e) {
			mainPanel.showToast(e.getMessage());
			e.printStackTrace();
			mainPanel.dispose();
			try {
				jcm.closeConnection();
			} catch (IOException | CadTransportException e1) {
				System.exit(0);
			}
		}
	}
}