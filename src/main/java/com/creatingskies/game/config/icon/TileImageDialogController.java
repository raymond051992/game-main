package com.creatingskies.game.config.icon;

import java.io.File;
import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import com.creatingskies.game.classes.ViewController;
import com.creatingskies.game.common.MainLayout;
import com.creatingskies.game.component.AlertDialog;
import com.creatingskies.game.core.TileImage;
import com.creatingskies.game.model.Constant;
import com.creatingskies.game.util.Util;

public class TileImageDialogController extends ViewController {

	@FXML private Slider difficultySlider;
	@FXML private TextField fileNameField;
	@FXML private ImageView previewImage;
	
	private Stage dialogStage;
	private TileImage tileImage;
	private Image defaultImage;
	
	private boolean saveClicked = false;
	private final String NO_FILE_MESSAGE = "Please choose a file.";
	
	public boolean show(TileImage tileImage) {
	    try {
	        FXMLLoader loader = new FXMLLoader();
	        loader.setLocation(getClass().getResource("TileImageDialog.fxml"));
	        AnchorPane page = (AnchorPane) loader.load();

	        Stage dialogStage = new Stage();
	        dialogStage.setTitle("TileImage");
	        dialogStage.initModality(Modality.WINDOW_MODAL);
	        dialogStage.initOwner(MainLayout.getPrimaryStage());
	        Scene scene = new Scene(page);
	        dialogStage.setScene(scene);

	        TileImageDialogController controller = loader.getController();
	        controller.setDialogStage(dialogStage);
	        controller.setTileImage(tileImage);
	        
	        dialogStage.showAndWait();
	        return controller.isSaveClicked();
	    } catch (IOException e) {
	        e.printStackTrace();
	        return false;
	    }
	}
	
	public void initialize() {
		super.init();
		fileNameField.setText(NO_FILE_MESSAGE);
		defaultImage = Util.byteArrayToImage(Util
				.stringUrlToByteArray(Constant.PATH_TILE_IMAGE_PLACEHOLDER));
	}
	
	public void setTileImage(TileImage tileImage) {
        this.tileImage = tileImage;
        
        difficultySlider.setDisable(tileImage.getDifficulty() == null);
        difficultySlider.setValue(tileImage.getDifficulty() != null ?
        		tileImage.getDifficulty() : 0.0);
        
        fileNameField.setText(tileImage.getFileName() != null
        		&& !tileImage.getFileName().equals("") ?
        		tileImage.getFileName() : NO_FILE_MESSAGE);
        
		previewImage.setImage(tileImage.getImage() != null ? Util.byteArrayToImage(tileImage.getImage()) : defaultImage);
    }
	
	public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }
	
	public boolean isSaveClicked() {
        return saveClicked;
    }
	
	@Override
	protected String getViewTitle() {
		return "Add Tile Image";
	}
	
	@FXML
	private void handleOpenBrowseDialog(){
		FileChooser fileChooser = new FileChooser();

        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
                "Image files", "*.jpeg", "*.jpg", "*.png", "*.bmp", "*.gif");
        fileChooser.getExtensionFilters().add(extFilter);

        File file = fileChooser.showOpenDialog(MainLayout.getPrimaryStage());
        
        if(file != null){
        	tileImage.setImage(Util.fileToByteArray(file));
            tileImage.setFileName(file != null ? file.getName() : null);
    		tileImage.setFileType(Util.getFileExtension(tileImage
    				.getFileName()));
    		tileImage.setFileSize(-1L);
        }
        
		fileNameField.setText(tileImage.getFileName() != null
        		&& !tileImage.getFileName().equals("") ?
        		tileImage.getFileName() : NO_FILE_MESSAGE);
		
		previewImage.setImage(tileImage.getImage() != null ? Util.byteArrayToImage(tileImage.getImage()) : defaultImage);
	}
	
	@FXML
    private void handleSave() {
        if (isInputValid()) {
            tileImage.setDifficulty((int) difficultySlider.getValue());
            saveClicked = true;
            dialogStage.close();
        }
    }
	
    @FXML
    private void handleCancel() {
        dialogStage.close();
    }
    
    private boolean isInputValid() {
        String errorMessage = "";

        if(fileNameField.getText().equals(NO_FILE_MESSAGE)){
        	errorMessage += "Image is required.\n";
        }

        if (errorMessage.length() == 0) {
            return true;
        } else {
			new AlertDialog(AlertType.ERROR, "Invalid fields",
					"Please correct invalid fields.", errorMessage)
					.showAndWait();
            return false;
        }
    }

}