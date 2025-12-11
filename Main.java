package proj;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

import java.awt.*;
import java.awt.event.*;
import java.awt.event.KeyListener;
public class Main {

	public static void main(String[] args) {
		Controller controller = new Controller();
        new View(controller);
	}

}
