package com.neuronrobotics.bowlerstudio.tabs;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.reactfx.util.FxTimer;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.Highlighter.HighlightPainter;

import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import javafx.event.EventHandler;
import javafx.scene.layout.VBox;

import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.scripting.IScriptEventListener;
import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine;
import com.neuronrobotics.bowlerstudio.scripting.ScriptingFileWidget;
import com.neuronrobotics.bowlerstudio.utils.FindTextWidget;

import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class LocalFileScriptTab extends VBox implements IScriptEventListener, EventHandler<WindowEvent> {
	private long lastRefresh = 0;
	private ScriptingFileWidget scripting;

	IScriptEventListener l = null;

	private MySwingNode swingNode;
	private RTextScrollPane spscrollPane;

	private Highlighter highlighter;

	private HighlightPainter painter;
	private int lineSelected = 0;

	private MyRSyntaxTextArea textArea = new MyRSyntaxTextArea(200, 300);

	private final File file;

	private Font myFont;

	private static HashMap<String, String> langaugeMapping = new HashMap<>();

	private class MySwingNode extends SwingNode {

		@Override
		public double prefWidth(double height) {
			try {
				return super.prefWidth(height);
			} catch (Exception e) {
				// System.out.println("Error in "+file);
				// e.printStackTrace();
				return prefWidthProperty().doubleValue();
			}

		}

		@Override
		public double prefHeight(double height) {
			try {
				return super.prefHeight(height);
			} catch (Exception e) {
				// System.out.println("Error in "+file);
				// e.printStackTrace();
				return prefHeightProperty().doubleValue();
			}

		}

		/**
		 * Returns the {@code SwingNode}'s minimum width for use in layout calculations.
		 * This value corresponds to the minimum width of the Swing component.
		 * 
		 * @return the minimum width that the node should be resized to during layout
		 */
		@Override
		public double minWidth(double height) {
			try {
				return super.minWidth(height);
			} catch (Exception e) {
				// System.out.println("Error in "+file);
				// e.printStackTrace();
				return minWidthProperty().doubleValue();
			}

		}

		/**
		 * Returns the {@code SwingNode}'s minimum height for use in layout
		 * calculations. This value corresponds to the minimum height of the Swing
		 * component.
		 * 
		 * @return the minimum height that the node should be resized to during layout
		 */
		@Override
		public double minHeight(double width) {
			try {
				return super.minHeight(width);
			} catch (Exception e) {
				// System.out.println("Error in "+file);
				// e.printStackTrace();
				return minHeightProperty().doubleValue();
			}
		}
	}

	public class MyRSyntaxTextArea extends RSyntaxTextArea implements ComponentListener {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public MyRSyntaxTextArea() {
			this.addComponentListener(this);
		}

		public MyRSyntaxTextArea(int i, int j) {
			super(i, j);
		}

		public void componentResized(ComponentEvent e) {
			System.err.println("componentResized");

		}

		public void componentHidden(ComponentEvent e) {
			System.err.println("componentHidden");
		}

		public void componentMoved(ComponentEvent e) {
			System.err.println("componentMoved");
		}

		public void componentShown(ComponentEvent e) {
			System.err.println("componentShown");
		}

	}

	public static void setExtentionSyntaxType(String shellType, String syntax) {
		langaugeMapping.put(shellType, syntax);
	}

	public LocalFileScriptTab(File file) throws IOException {

		this.file = file;
		setScripting(new ScriptingFileWidget(file));
		setSpacing(5);
		l = this;

		getScripting().addIScriptEventListener(l);
		String type;

		String shellType = ScriptingEngine.getShellType(file.getName());
		switch (shellType) {
		case "Clojure":
			type = SyntaxConstants.SYNTAX_STYLE_CLOJURE;
			break;
		default:
			type = langaugeMapping.get(shellType);
			if (type == null) {
				type = SyntaxConstants.SYNTAX_STYLE_NONE;
				if (shellType.toLowerCase().contains("arduino")) {
					type = SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS;
				}
			}
		case "JSON":
			type = SyntaxConstants.SYNTAX_STYLE_JSON;
			break;
		case "ArduingScriptingLangauge":
			type = SyntaxConstants.SYNTAX_STYLE_LISP;
			break;
		case "Arduino":
			type = SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS;
			break;
		case "Groovy":
			type = SyntaxConstants.SYNTAX_STYLE_GROOVY;
			break;
		case "Jython":
			type = SyntaxConstants.SYNTAX_STYLE_PYTHON;
			break;
		case "MobilBaseXML":
			type = SyntaxConstants.SYNTAX_STYLE_XML;
			break;

		}
		textArea.setSyntaxEditingStyle(type);
		textArea.setCodeFoldingEnabled(true);

		textArea.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void removeUpdate(DocumentEvent e) {

			}

			@Override
			public void insertUpdate(DocumentEvent e) {

			}

			@Override
			public void changedUpdate(DocumentEvent arg0) {
				System.err.println("changedUpdate " + file);
				new Thread() {
					public void run() {
						getScripting().removeIScriptEventListener(l);
						getScripting().setCode(textArea.getText());
						getScripting().addIScriptEventListener(l);
					}
				}.start();
			}
		});
		textArea.addCaretListener(new CaretListener() {

			@Override
			public void caretUpdate(CaretEvent e) {

				// Lets start with some default values for the line and column.
				int linenum = 1;
				int columnnum = 1;

				// We create a try catch to catch any exceptions. We will simply
				// ignore such an error for our demonstration.
				try {
					// First we find the position of the caret. This is the
					// number of where the caret is in relation to the start of
					// the JTextArea
					// in the upper left corner. We use this position to find
					// offset values (eg what line we are on for the given
					// position as well as
					// what position that line starts on.
					int caretpos = textArea.getCaretPosition();
					linenum = textArea.getLineOfOffset(caretpos);

					// We subtract the offset of where our line starts from the
					// overall caret position.
					// So lets say that we are on line 5 and that line starts at
					// caret position 100, if our caret position is currently
					// 106
					// we know that we must be on column 6 of line 5.
					columnnum = caretpos - textArea.getLineStartOffset(linenum);

					// We have to add one here because line numbers start at 0
					// for getLineOfOffset and we want it to start at 1 for
					// display.
					linenum += 1;
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				if (lineSelected != linenum) {
					lineSelected = linenum;
					// System.err.println("Select "+lineSelected);
					new Thread(() -> {
						BowlerStudio.select(file, lineSelected);
					}).start();
				}

			}

		});

		textArea.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() > 2) {
					highlighter.removeAllHighlights();
				}
				// System.out.println("Number of click: " + e.getClickCount());
				// System.out.println("Click position (X, Y): " + e.getX() + ",
				// " + e.getY());
			}
		});

		spscrollPane = new RTextScrollPane(textArea);

		swingNode = new MySwingNode();

		SwingUtilities.invokeLater(() -> swingNode.setContent(spscrollPane));

		getScripting().setFocusTraversable(false);

		getChildren().setAll(swingNode, getScripting());
		swingNode.setOnMouseEntered(mouseEvent -> {
			//System.err.println("On mouse entered " + file.getName());
			resizeEvent();
		});
		// textArea
		KeyStroke keystroke = KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_MASK);
		textArea.getInputMap().put(keystroke, "f");
		textArea.getActionMap().put("f", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {

				findTextWidget();

			}
		});

		myFont = textArea.getFont();
		highlighter = textArea.getHighlighter();
		painter = new DefaultHighlighter.DefaultHighlightPainter(Color.pink);

		highlighter.removeAllHighlights();
		focusedProperty().addListener((w, o, n) -> {
			System.err.println("Focused " + file);
		});

		widthProperty().addListener((w, o, n) -> {
			//resizeEvent();
//			FxTimer.runLater(Duration.ofMillis((int) (40.0 * Math.random())), () -> {
//				getChildren().clear();
//				getChildren().setAll(swingNode, getScripting());
//				//resizeEvent();
//			});
		});
		heightProperty().addListener((w, o, n) -> {
			//resizeEvent();
		});
		SwingUtilities.invokeLater(() -> {
			if (getScripting() != null && getScripting().getCode() != null) {
				onScriptChanged(null, getScripting().getCode(), file);
			}

		});
	}

	private void resizeEvent() {
		if (!((lastRefresh + 16) < System.currentTimeMillis())) {
//			FxTimer.runLater(Duration.ofMillis((int) (20.0 * Math.random() + 16)), () -> {
//				//resizeEvent();
//			});
			return;
		}
		lastRefresh = System.currentTimeMillis();
		//System.err.println("resizeEvent " + file.getName());

//		SwingUtilities.invokeLater(() -> {
//			spscrollPane.setSize((int) spscrollPane.getWidth(), (int) spscrollPane.getHeight());
//		});
//		SwingUtilities.invokeLater(() -> {
//			spscrollPane.invalidate();
//		});
//		SwingUtilities.invokeLater(() -> {
//			spscrollPane.repaint();
//		});
//		SwingUtilities.invokeLater(() -> {
//			textArea.requestFocusInWindow();
//		});
//		SwingUtilities.invokeLater(() -> {
//			textArea.invalidate();
//		});
//		SwingUtilities.invokeLater(() -> {
//			textArea.repaint();
//		});
		swingNode.setContent(spscrollPane);
//		FxTimer.runLater(Duration.ofMillis((int) 16), () -> {
//			swingNode.requestFocus();
//		});

		// System.err.println("resize "+file);
	}

	@Override
	public void onScriptFinished(Object result, Object previous, File source) {
		// // TODO Auto-generated method stub
		// SwingUtilities.invokeLater(new Runnable() {
		// @Override
		// public void run() {
		// textArea.requestFocusInWindow();
		// }
		// });

	}

	@Override
	public void onScriptChanged(String previous, String current, File source) {
		// int place = textArea.getCaretPosition();
		// System.err.println("Carrot position is= "+place);
		// codeArea.replaceText(current);
		// codeArea.setCursor(place);

		if (current.length() > 3 && !textArea.getText().contentEquals(current)) {// no
																					// empty
																					// writes
			SwingUtilities.invokeLater(() -> {
				textArea.setText(current);
				if (previous == null)
					SwingUtilities.invokeLater(() -> textArea.setCaretPosition(0));
			});

		}
	}

	@Override
	public void onScriptError(Throwable except, File source) {
		// TODO Auto-generated method stub
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				System.out.println("script error");
				textArea.requestFocusInWindow();
			}
		});

	}

	@SuppressWarnings("restriction")
	public void findTextWidget() {
		Platform.runLater(() -> {
			Stage s = new Stage();
			new Thread() {
				public void run() {

					FindTextWidget controller = new FindTextWidget();
					controller.setTextArea(textArea);
					try {
						controller.start(s);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}.start();
		});
		// DeviceManager.addConnection();
	}

	@Override
	public void handle(WindowEvent event) {
		// TODO Auto-generated method stub
		getScripting().stop();
	}

	public ScriptingFileWidget getScripting() {
		return scripting;
	}

	public void setScripting(ScriptingFileWidget scripting) {
		this.scripting = scripting;
	}

	public void setHighlight(int lineNumber, Color color) throws BadLocationException {
		if (textArea == null) {
			return;
		}
		painter = new DefaultHighlighter.DefaultHighlightPainter(color);
		int startIndex = textArea.getLineStartOffset(lineNumber - 1);
		int endIndex = textArea.getLineEndOffset(lineNumber - 1);

		try {

			SwingUtilities.invokeLater(() -> {
				textArea.moveCaretPosition(startIndex);
			});
		} catch (Error | Exception ex) {
			ex.printStackTrace();
		}
		SwingUtilities.invokeLater(() -> {
			try {
				textArea.getHighlighter().addHighlight(startIndex, endIndex, painter);
			} catch (BadLocationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});

	}

	public void clearHighlits() {
		highlighter.removeAllHighlights();
	}

	public int getFontSize() {
		return myFont.getSize();
	}

	public void setFontSize(int size) {
		myFont = new Font(myFont.getName(), myFont.getStyle(), size);
		setFontLoop();
	}

	private void setFontLoop() {
		FxTimer.runLater(Duration.ofMillis(200), () -> {
			try {
				SwingUtilities.invokeLater(() -> {
					textArea.setFont(myFont);
				});
			} catch (Throwable ex) {
				System.err.println("Tab Font set failed " + file.getAbsolutePath());
				setFontLoop();
			}
		});
	}
}
