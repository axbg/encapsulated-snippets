package ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.sun.javacard.apduio.CadTransportException;

import io.IOManager;
import io.JCardManager;
import io.utils.AESMode;
import io.utils.JCardException;

public class MainPanel extends JFrame {
	private static final long serialVersionUID = -5613412694551256610L;

	private static final String PASSWORD_BTN_SET = "Set";
	private static final String PASSWORD_BTN_RESET = "Reset";
	private static final String START_PROCESS = "Start process";
	private static final String LOADING_PICTURE = "loading.gif";
	private static final String PASSWORD_SET = "The password is set";
	private static final String PASSWORD_UNSET = "The password is not set";
	private static final Color MATERIAL_GREEN = new Color(67, 160, 71);

	private IOManager io;
	private JCardManager jcm;

	private File selectedFile;
	private byte[] password = null;
	private boolean passwordSet = false;

	private JTextField passwordField;
	private JLabel passwordStatus;
	private JButton passwordBtn;
	private JLabel fileLabel;
	private JFileChooser browseFile;
	private JButton browseFileBtn;
	private JRadioButton encryptMode;
	private JRadioButton decryptMode;
	private JButton startProcessBtn;
	private JLabel loadingPicture;

	public MainPanel(IOManager io) {
		super("JCard AES");
		this.io = io;
		this.initUILayout();
		this.initFields();
		this.setVisible(true);
	}

	public void setJcm(JCardManager jcm) {
		this.jcm = jcm;
	}

	private void initUILayout() {
		setSize(500, 340);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(null);
		this.setLocationRelativeTo(null);
	}

	private void initFields() {
		passwordField = new JTextField(16);
		passwordField.setBounds(170, 15, 110, 20);

		JLabel password = new JLabel("Password");
		password.setLabelFor(passwordField);
		password.setBounds(90, 10, 100, 30);

		passwordBtn = new JButton(PASSWORD_BTN_SET);
		passwordBtn.setBounds(300, 15, 100, 20);
		passwordBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setPasswordBtnClicked();
			}
		});

		passwordStatus = new JLabel(PASSWORD_UNSET);
		passwordStatus.setForeground(Color.RED);
		passwordStatus.setBounds(160, 40, 200, 20);

		fileLabel = new JLabel("");
		fileLabel.setBounds(50, 100, 400, 20);

		browseFile = new JFileChooser();

		browseFileBtn = new JButton("Select file");
		browseFileBtn.setBounds(160, 130, 150, 30);
		browseFileBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				chooseFileBtnClicked();
			}
		});

		encryptMode = new JRadioButton("Encrypt");
		encryptMode.setBounds(165, 180, 80, 20);
		encryptMode.setSelected(true);

		decryptMode = new JRadioButton("Decrypt");
		decryptMode.setBounds(245, 180, 80, 20);

		ButtonGroup modeGroup = new ButtonGroup();
		modeGroup.add(encryptMode);
		modeGroup.add(decryptMode);

		startProcessBtn = new JButton(START_PROCESS);
		startProcessBtn.setVisible(false);
		startProcessBtn.setBounds(160, 220, 150, 30);
		startProcessBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				startProcessBtnClicked();
			}
		});

		Icon loadingPictureFile = null;
		loadingPictureFile = new ImageIcon(ClassLoader.getSystemClassLoader().getResource(LOADING_PICTURE));

		try {
			loadingPicture = new JLabel(loadingPictureFile);
		} catch (Exception ex) {
			showToast("Resource not found: " + LOADING_PICTURE);
		}

		loadingPicture.setBounds(200, 250, 80, 50);
		loadingPicture.setVisible(false);

		this.add(passwordField);
		this.add(password);
		this.add(passwordBtn);
		this.add(passwordStatus);
		this.add(fileLabel);
		this.add(browseFile);
		this.add(browseFileBtn);
		this.add(encryptMode);
		this.add(decryptMode);
		this.add(startProcessBtn);
		this.add(loadingPicture);

		handleWindowClosed();
	}

	private void setButtonsState(boolean state) {
		this.loadingPicture.setVisible(!state);
		for (Component cp : this.getContentPane().getComponents()) {
			if (!(cp instanceof JLabel)) {
				cp.setEnabled(state);
			}
		}
	}

	private byte[] getDerivedPassword(String password) {
		byte[] derivedKey = null;

		try {
			// in a typical system the salt should be randomly generated and stored
			byte[] salt = "salt".getBytes();

			KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 10000, 128);
			SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

			derivedKey = f.generateSecret(spec).getEncoded();
		} catch (NoSuchAlgorithmException e) {
			showToast("PBKDF2WithHmacSHA256 is not supported");
			System.exit(-1);
		} catch (InvalidKeySpecException e) {
			showToast("Invalid KeySpec");
			System.exit(-1);
		}

		return derivedKey;
	}

	private void setPasswordBtnClicked() {
		try {
			if (!passwordSet) {
				if (this.passwordField.getText().isBlank()) {
					showToast("Password should contain at least 1 character other than blank");
					return;
				}

				this.password = getDerivedPassword(this.passwordField.getText());

				this.jcm.setMode(AESMode.SET_PASSWORD);
				this.jcm.exchangeCommand(this.jcm.prepareCommand(this.password, true));

				this.passwordSet = true;
				this.passwordBtn.setText(PASSWORD_BTN_RESET);
				this.passwordStatus.setText(PASSWORD_SET);
				this.passwordStatus.setForeground(MATERIAL_GREEN);
			} else {
				this.jcm.setMode(AESMode.RESET_PASSWORD);
				this.jcm.exchangeCommand(this.jcm.prepareCommand(null, true));

				this.password = null;
				this.passwordSet = false;
				this.passwordBtn.setText(PASSWORD_BTN_SET);
				this.passwordStatus.setText(PASSWORD_UNSET);
				this.passwordStatus.setForeground(Color.RED);
				this.passwordField.setText("");
				this.fileLabel.setText("");
			}
		} catch (IOException | CadTransportException | JCardException e) {
			this.showToast(e.getLocalizedMessage());
			e.printStackTrace();
			return;
		}
	}

	private void chooseFileBtnClicked() {
		if (this.password == null) {
			showToast("Set password before choosing a file!");
			return;
		}

		if (browseFile.showOpenDialog(MainPanel.this) == JFileChooser.APPROVE_OPTION) {
			selectedFile = browseFile.getSelectedFile();
			fileLabel.setText(selectedFile.getAbsolutePath());
			fileLabel.setForeground(Color.BLACK);
			this.io.setInputFile(selectedFile);
			this.startProcessBtn.setVisible(true);
		}
	}

	public void startProcessBtnClicked() {
		jcm.setMode(encryptMode.isSelected() ? AESMode.ENCRYPT : AESMode.DECRYPT);

		this.setButtonsState(false);
		this.startProcessBtn.setText("Processing...");

		Thread processingThread = new Thread() {
			@Override
			public void run() {
				runProcess();
			}
		};

		processingThread.start();
	}

	public void runProcess() {
		try {
			this.io.initializeStreams(encryptMode.isSelected() ? true : false);

			byte[] currentChunk = null;
			while ((currentChunk = this.io.readChunk()).length != 0) {
				byte[] preparedCommand = this.jcm.prepareCommand(currentChunk, io.isLastBlock());
				byte[] encryptedChunk = this.jcm.exchangeCommand(preparedCommand);
				this.io.writeChunk(encryptedChunk);
			}

			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					setButtonsState(true);
					fileLabel.setForeground(MATERIAL_GREEN);
					startProcessBtn.setText(START_PROCESS);

					File encryptedFile = new File(fileLabel.getText());
					try {
						Desktop.getDesktop().open(encryptedFile.getParentFile());
					} catch (IOException e) {
						showToast("Encrypted file was not found.");
						e.printStackTrace();
					}
				}
			});
		} catch (IOException | CadTransportException | JCardException e) {
			this.showToast(e.getLocalizedMessage());
			e.printStackTrace();
			return;
		} finally {
			setButtonsState(true);
			try {
				this.io.closeStreams();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void handleWindowClosed() {
		this.addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(WindowEvent winEvt) {
				try {
					jcm.closeConnection();
					io.closeStreams();
				} catch (IOException | CadTransportException e) {
				} finally {
					System.exit(0);
				}
			}
		});
	}

	public void showToast(String message) {
		JOptionPane.showMessageDialog(this, message);
	}
}