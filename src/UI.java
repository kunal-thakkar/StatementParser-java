
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;

public class UI extends JPanel implements ActionListener {
	
	private static final long serialVersionUID = 6784806604790776756L;
	static private final String newline = "\n";
	private JTextField password;
    private JComboBox<Parser.StatementTypes> statementList;
	private JButton openButton, saveButton;
	private JTextArea log;
	private JFileChooser fc;
	private File statements[] = new File[0];
    
	public UI(){
		super(new BorderLayout());
		
		log = new JTextArea(5,20);
        log.setMargin(new Insets(5,5,5,5));
        log.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(log);
        
        fc = new JFileChooser();
        fc.setMultiSelectionEnabled(true);
        
        //fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        //fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        
        openButton = new JButton("Open a File...");
        openButton.addActionListener(this);
        
        saveButton = new JButton("Extract Transactions");
        saveButton.addActionListener(this);
        

        //Create the combo box, select item at index 4.
        statementList = new JComboBox<Parser.StatementTypes>(Parser.StatementTypes.values());
        statementList.setSelectedIndex(0);
        statementList.addActionListener(this);
        
        password = new JTextField();
        password.setText("Enter Password Here");
        password.setSize(321, 100);
                
        //For layout purposes, put the buttons in a separate panel
        JPanel buttonPanel = new JPanel(); //use FlowLayout
        buttonPanel.add(statementList);
        buttonPanel.add(password);
        buttonPanel.add(openButton);
        buttonPanel.add(saveButton);
        
        //Add the buttons and the log to this panel.
        add(buttonPanel, BorderLayout.PAGE_START);
        add(logScrollPane, BorderLayout.CENTER);
	}

	/**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event dispatch thread.
     */
    private static void createAndShowGUI() {
        //Create and set up the window.
        JFrame frame = new JFrame("Statement Parser");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //Add content to the window.
        frame.add(new UI());
 
        
        //Display the window.
        frame.setSize(800, 500);
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation(dim.width/2-frame.getSize().width/2, dim.height/2-frame.getSize().height/2);
        //frame.pack();
        frame.setVisible(true);
    }
    
	public static void main(String[] a) throws InvalidPasswordException, FileNotFoundException, IOException{
		SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                //Turn off metal's use of bold fonts
                UIManager.put("swing.boldMetal", Boolean.FALSE); 
                createAndShowGUI();
            }
        });
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		 //Handle open button action.
        if (e.getSource() == openButton) {
            int returnVal = fc.showOpenDialog(UI.this); 
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                statements = fc.getSelectedFiles();
            } else {
                log.append("Open command cancelled by user." + newline);
            }
            log.setCaretPosition(log.getDocument().getLength()); 
        //Handle save button action.
        } else if (e.getSource() == saveButton) {
        	Parser.parseStatements(statements, password.getText(), (Parser.StatementTypes)statementList.getSelectedItem(), new Parser.Logger() {
				@Override
				public void log(String str) {
					log.append(str + newline);
				}
			});
        }
	}

}
//