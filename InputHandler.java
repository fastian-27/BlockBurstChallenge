package input;

import blocked.model.Game;
import blocked.ui.GamePanel;
import blocked.ui.SoundManager;
import blocked.model.BlockPiece;
import java.awt.event.*;
import javax.swing.SwingUtilities;

/**
 * Handles all user input for the game including mouse and keyboard controls.
 * Supports both mouse drag-and-drop and keyboard navigation.
 * 
 * @author Saim Arif Khan
 */
public class InputHandler extends MouseAdapter implements KeyListener {
    
    private Game game;
    private GamePanel gamePanel;
    
    // Mouse control state
    private int draggedSlot = -1;  // Which tray slot is being dragged (-1 = none)
    private int mouseX, mouseY;
    private boolean isDragging = false;
    
    // Keyboard control state
    private int selectedSlot = -1;  // Which tray slot is selected (-1 = none)
    private int previewRow = 0;
    private int previewCol = 0;
    private boolean keyboardMode = false;
    
    // Layout constants (will be set by GamePanel)
    private int boardX, boardY, boardSize, cellSize;
    private int trayY, traySlotWidth;
    
    /**
     * Creates a new InputHandler for the game.
     * 
     * @param game The game instance to control
     * @param gamePanel The panel to update when input occurs
     * @throws IllegalArgumentException if game or gamePanel is null
     */
    public InputHandler(Game game, GamePanel gamePanel) {
        if (game == null) {
            throw new IllegalArgumentException("Game instance cannot be null");
        }
        if (gamePanel == null) {
            throw new IllegalArgumentException("GamePanel instance cannot be null");
        }
        
        this.game = game;
        this.gamePanel = gamePanel;
    }
    
    /**
     * Sets the layout dimensions for input calculations.
     * 
     * @throws IllegalArgumentException if any dimension is negative
     */
    public void setLayoutDimensions(int boardX, int boardY, int boardSize, int cellSize,
                                   int trayY, int traySlotWidth) {
        if (boardSize < 0 || cellSize < 0 || traySlotWidth < 0) {
            throw new IllegalArgumentException("Dimensions cannot be negative");
        }
        
        this.boardX = boardX;
        this.boardY = boardY;
        this.boardSize = boardSize;
        this.cellSize = cellSize;
        this.trayY = trayY;
        this.traySlotWidth = traySlotWidth;
    }
    
    // ==================== MOUSE CONTROLS ====================
    
    @Override
    public void mousePressed(MouseEvent e) {
        try {
            if (!SwingUtilities.isLeftMouseButton(e)) return;
            
            // Check if clicking on a tray piece
            int slot = getTraySlotAt(e.getX(), e.getY());
            if (slot >= 0 && slot < 3) {
                BlockPiece piece = game.getTrayPiece(slot);
                if (piece != null) {
                    draggedSlot = slot;
                    mouseX = e.getX();
                    mouseY = e.getY();
                    isDragging = false;
                    keyboardMode = false;  // Switch to mouse mode
                    selectedSlot = -1;
                    SoundManager.getInstance().playPieceSelect();
                }
            }
            
            // Check if clicking on menu buttons
            gamePanel.handleMenuClick(e.getX(), e.getY());
            
        } catch (Exception ex) {
            System.err.println("Error handling mouse press: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    @Override
    public void mouseDragged(MouseEvent e) {
        if (draggedSlot != -1) {
            mouseX = e.getX();
            mouseY = e.getY();
            isDragging = true;
            gamePanel.setDragState(draggedSlot, mouseX, mouseY);
            gamePanel.repaint();
        }
    }
    
    @Override
    public void mouseReleased(MouseEvent e) {
        if (draggedSlot == -1) return;
        
        try {
            // Try to place the piece at the board position
            int[] gridPos = getBoardCellAt(e.getX(), e.getY());
            if (gridPos != null && gridPos.length == 2) {
                int row = gridPos[0];
                int col = gridPos[1];
                
                // Validate row and col are within bounds
                if (row >= 0 && row < 8 && col >= 0 && col < 8) {
                    if (game.canPlace(draggedSlot, row, col)) {
                        gamePanel.placePiece(draggedSlot, row, col);
                    } else {
                        SoundManager.getInstance().playInvalidPlacement();
                    }
                } else {
                    SoundManager.getInstance().playInvalidPlacement();
                }
            } else {
                SoundManager.getInstance().playInvalidPlacement();
            }
            
        } catch (Exception ex) {
            System.err.println("Error handling mouse release: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            // Always reset drag state, even if error occurred
            draggedSlot = -1;
            isDragging = false;
            gamePanel.clearDragState();
            gamePanel.repaint();
        }
    }
    
    @Override
    public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        
        // Update hover state for visual feedback
        if (!keyboardMode) {
            gamePanel.setMousePosition(mouseX, mouseY);
        }
    }
    
    // ==================== KEYBOARD CONTROLS ====================
    
    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        try {
            
            // Global controls (work anytime)
            if (key == KeyEvent.VK_P) {
                gamePanel.togglePause();
                return;
            }
            if (key == KeyEvent.VK_R) {
                gamePanel.restart();
                return;
            }
            if (key == KeyEvent.VK_M) {
                SoundManager.getInstance().toggleMute();
                gamePanel.repaint();
                return;
            }
            if ((key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) && !keyboardMode) {
                gamePanel.handleMenuKey();
                return;
            }
        
            // Piece selection (1, 2, 3)
            if (key >= KeyEvent.VK_1 && key <= KeyEvent.VK_3) {
                int slot = key - KeyEvent.VK_1;
                
                if (slot >= 0 && slot < 3) {
                    BlockPiece piece = game.getTrayPiece(slot);
                    if (piece != null) {
                        selectedSlot = slot;
                        keyboardMode = true;
                        draggedSlot = -1;  // Cancel any mouse drag
                        previewRow = 3;    // Start in center of board
                        previewCol = 3;
                        SoundManager.getInstance().playPieceSelect();
                        gamePanel.setKeyboardState(selectedSlot, previewRow, previewCol);
                        gamePanel.repaint();
                    }
                }
                return;
            }
        
            // If a piece is selected with keyboard, allow movement
            if (keyboardMode && selectedSlot >= 0 && selectedSlot < 3) {
                boolean moved = false;
                
                switch (key) {
                    case KeyEvent.VK_UP:
                        if (previewRow > 0) {
                            previewRow--;
                            moved = true;
                        }
                        break;
                    case KeyEvent.VK_DOWN:
                        if (previewRow < 7) {
                            previewRow++;
                            moved = true;
                        }
                        break;
                    case KeyEvent.VK_LEFT:
                        if (previewCol > 0) {
                            previewCol--;
                            moved = true;
                        }
                        break;
                    case KeyEvent.VK_RIGHT:
                        if (previewCol < 7) {
                            previewCol++;
                            moved = true;
                        }
                        break;
                    case KeyEvent.VK_ENTER:
                    case KeyEvent.VK_SPACE:
                        // Try to place the piece
                        if (previewRow >= 0 && previewRow < 8 && previewCol >= 0 && previewCol < 8) {
                            if (game.canPlace(selectedSlot, previewRow, previewCol)) {
                                gamePanel.placePiece(selectedSlot, previewRow, previewCol);
                                selectedSlot = -1;
                                keyboardMode = false;
                                gamePanel.clearKeyboardState();
                            } else {
                                SoundManager.getInstance().playInvalidPlacement();
                            }
                        }
                        break;
                    case KeyEvent.VK_ESCAPE:
                        // Cancel selection
                        selectedSlot = -1;
                        keyboardMode = false;
                        gamePanel.clearKeyboardState();
                        gamePanel.repaint();
                        break;
                }
                
                if (moved) {
                    gamePanel.setKeyboardState(selectedSlot, previewRow, previewCol);
                    gamePanel.repaint();
                }
            }
            } catch (Exception ex) {
            System.err.println("Error handling key press: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    @Override
    public void keyReleased(KeyEvent e) {
        // Not needed for this implementation
    }
    
    @Override
    public void keyTyped(KeyEvent e) {
        // Not needed for this implementation
    }
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Determines which tray slot (0, 1, 2) is at the given screen coordinates.
     * 
     * @return slot index (0-2) or -1 if not over a tray slot
     */
    private int getTraySlotAt(int x, int y) {
        try {
            if (traySlotWidth <= 0) return -1;
            
            int trayHeight = 120;  // Approximate tray height
            if (y < trayY || y > trayY + trayHeight) {
                return -1;
            }
            
            // Calculate which slot based on x position
            int panelWidth = gamePanel.getWidth();
            if (panelWidth <= 0) return -1;
            
            int trayStartX = (panelWidth - traySlotWidth * 3) / 2;
            
            for (int i = 0; i < 3; i++) {
                int slotX = trayStartX + i * traySlotWidth;
                if (x >= slotX && x < slotX + traySlotWidth) {
                    return i;
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error calculating tray slot: " + e.getMessage());
        }
        
        return -1;
    }
    
    /**
     * Converts screen coordinates to board grid position.
     * 
     * @return int[2] array with [row, col] or null if not over board
     */
    private int[] getBoardCellAt(int x, int y) {
        try {
            if (cellSize <= 0) return null;
            
            if (x < boardX || x >= boardX + boardSize ||
                y < boardY || y >= boardY + boardSize) {
                return null;
            }
            
            int col = (x - boardX) / cellSize;
            int row = (y - boardY) / cellSize;
            
            if (row >= 0 && row < 8 && col >= 0 && col < 8) {
                return new int[] { row, col };
            }
            
        } catch (ArithmeticException e) {
            System.err.println("Error calculating board cell (division error): " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error calculating board cell: " + e.getMessage());
        }
        
        return null;
    }
    
    // ==================== GETTERS ====================
    
    public boolean isDragging() {
        return isDragging;
    }
    
    public int getDraggedSlot() {
        return draggedSlot;
    }
    
    public int getMouseX() {
        return mouseX;
    }
    
    public int getMouseY() {
        return mouseY;
    }
    
    public boolean isKeyboardMode() {
        return keyboardMode;
    }
    
    public int getSelectedSlot() {
        return selectedSlot;
    }
    
    public int getPreviewRow() {
        return previewRow;
    }
    
    public int getPreviewCol() {
        return previewCol;
    }
}