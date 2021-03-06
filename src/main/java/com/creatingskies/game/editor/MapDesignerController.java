package com.creatingskies.game.editor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.creatingskies.game.classes.ViewController.Action;
import com.creatingskies.game.common.MainLayout;
import com.creatingskies.game.component.AlertDialog;
import com.creatingskies.game.config.icon.TileImageDialogController;
import com.creatingskies.game.config.obstacle.ObstacleDialogController;
import com.creatingskies.game.core.Game;
import com.creatingskies.game.core.Map;
import com.creatingskies.game.core.MapDao;
import com.creatingskies.game.core.Tile;
import com.creatingskies.game.core.TileImage;
import com.creatingskies.game.model.Constant;
import com.creatingskies.game.model.obstacle.Obstacle;
import com.creatingskies.game.model.obstacle.ObstacleDAO;
import com.creatingskies.game.util.Util;

import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class MapDesignerController {
	
	@FXML private AnchorPane mapContainer;
	
	@FXML private GridPane mapTiles;
	@FXML private Label viewTitle;
	@FXML private Label selectedTileDescription;
	
	@FXML private FlowPane tileImageSelections;
	@FXML private FlowPane obstacleImageSelections;
	@FXML private FlowPane requiredTileSelections;
	@FXML private FlowPane startAndFinishPane;
	
	@FXML private ImageView selectedTileImageView;
	
	@FXML private Button saveButton;
	@FXML private Button cancelButton;
	
	@FXML private Button moveToArchiveButton;
	@FXML private Button viewArchivesButton;;
	
	private Obstacle selectedObstacle;
	private TileImage selectedTileImage;
	
	private ImageView startTileImageView;
	private ImageView endTileImageView;
	
	private boolean startTileSelected;
	private boolean endTileSelected;
	private boolean isEditable;
	
	private Game game;
	private Stage stage;
	
	private Action currentAction;
	private boolean copyFromOtherMap;
	
	private List<TileImage> selectedTileImages = new ArrayList<TileImage>();
	
	public void init(){
		isEditable = (getCurrentAction() == Action.VIEW);
		saveButton.setVisible(!isEditable);
		cancelButton.setText(isEditable ? "OK" : "Cancel");
		selectedTileDescription.setText("");
		
		initMapTiles();
		initTileImageSelections();
		initObstacleSelections();
		initRequiredTiles();
		validateRequiredTiles();
	}
	
	private void initMapTiles(){
		mapTiles.getChildren().clear();
		
		for(Tile tile : getMap().getTiles()){
			ImageView backImage = new ImageView();
			backImage.setFitHeight(Constant.TILE_HEIGHT);
			backImage.setFitWidth(Constant.TILE_WIDTH);
			backImage.setImage(Util.byteArrayToImage(tile.getBackImage() != null ?
					tile.getBackImage().getImage() : getMap().getDefaultTileImage().getImage()));
			
			ImageView frontImage = new ImageView();
			frontImage.setFitHeight(Constant.TILE_HEIGHT);
			frontImage.setFitWidth(Constant.TILE_WIDTH);
			
			if(tile.getObstacle() != null || tile.getStartPoint() || tile.getEndPoint()){
				frontImage.setImage(Util.byteArrayToImage(tile.getObstacle() != null ?
						tile.getObstacle().getImage() : tile.getFrontImage().getImage()));
			}
			
			Group group = new Group(backImage, frontImage);
			mapTiles.add(group, tile.getColIndex(), tile.getRowIndex());
			
			if(!isEditable){
				addHandler(tile, backImage, frontImage);
			}
		}
	}

	private void addHandler(Tile tile, ImageView backImage, ImageView frontImage) {
		backImage.setOnMousePressed(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				if(event.isControlDown()){
					handleRemoveFrontImage(tile, frontImage, backImage);
				} else {
					handlePaintTile(tile, frontImage, backImage);
				}
				
				if(event.getClickCount() == 2 && tile.getObstacle() != null){
					showObstacleDialog(tile);
				}
			}
		});
		
		backImage.setOnMouseEntered(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				if(event.isControlDown()){
					handleRemoveFrontImage(tile, frontImage, backImage);
				} else if(event.isAltDown()){
					handlePaintTile(tile, frontImage, backImage);
				}
			}
		});
		
		frontImage.setOnMousePressed(backImage.getOnMousePressed());
		frontImage.setOnMouseEntered(backImage.getOnMouseEntered());
	}
	
	private void handleRemoveFrontImage(Tile tile, ImageView frontImage, ImageView backImage){
		tile.setObstacle(null);
		tile.setFrontImage(null);
		tile.setStartPoint(false);
		tile.setEndPoint(false);
		frontImage.setImage(null);
		validateRequiredTiles();
	}
	
	private void handlePaintTile(Tile tile, ImageView frontImage, ImageView backImage) {
		if(getCurrentAction() != null && (selectedTileImage != null || selectedObstacle != null)){
			if(tile.getObstacle() == null){
				tile.setObstacleFields(selectedObstacle);
			}
			
			if(selectedObstacle != null){
				frontImage.setImage(Util.byteArrayToImage(selectedObstacle.getImage()));
				tile.setFrontImage(null);
				tile.setStartPoint(false);
				tile.setEndPoint(false);
			} else if (startTileSelected || endTileSelected) {
				tile.setFrontImage(selectedTileImage);
				frontImage.setImage(Util.byteArrayToImage(selectedTileImage.getImage()));
				selectedTileImageView.setImage(null);
				selectedTileImage = null;
				tile.setStartPoint(startTileSelected);
				tile.setEndPoint(endTileSelected);
				selectedTileDescription.setText("");
			} else {
				backImage.setImage(Util.byteArrayToImage(selectedTileImage.getImage()));
				tile.setBackImage(selectedTileImage);
			}
			
			validateRequiredTiles();
		}
	}
	
	private void validateRequiredTiles(){
		if(getMap().getStartPoint() != null){
			requiredTileSelections.getChildren().remove(startTileImageView);
		} else if(!requiredTileSelections.getChildren().contains(startTileImageView)){
			requiredTileSelections.getChildren().add(startTileImageView);
		}
		
		if(getMap().getEndPoint() != null){
			requiredTileSelections.getChildren().remove(endTileImageView);
		} else if(!requiredTileSelections.getChildren().contains(endTileImageView)){
			requiredTileSelections.getChildren().add(endTileImageView);
		}
		
		startAndFinishPane.setVisible(!requiredTileSelections.getChildren().isEmpty());
	}
	
	private void initTileImageSelections(){
		tileImageSelections.getChildren().clear();
		MapDao mapDao = new MapDao();
		List<TileImage> tileImages = mapDao.findAllTileImages(false, false);
		tileImages.add(getMap().getDefaultTileImage());
		
		if(tileImages != null && !tileImages.isEmpty()){
			for(TileImage tileImage : tileImages){
				addTileImageSelection(tileImage);
			}
		}
		addTileUploadImage();
	}
	
	private void addTileUploadImage(){
		ImageView imageView = new ImageView("/images/tiles/quickadd.png");
		imageView.setFitHeight(Constant.TILE_HEIGHT);
		imageView.setFitWidth(Constant.TILE_WIDTH);
		
		Pane pane = new Pane(imageView);
		
		if(!isEditable){
			pane.setOnMouseClicked(new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent event) {
					uploadTileImage();
				}
			});
		}
		
		tileImageSelections.getChildren().add(pane);
	}
	
	private void initObstacleSelections(){
		obstacleImageSelections.getChildren().clear();
		ObstacleDAO obstacleDAO = new ObstacleDAO();
		List<Obstacle> obstacles = obstacleDAO.findAll();
		
		if(obstacles != null && !obstacles.isEmpty()){
			for(Obstacle obstacle : obstacles){
				addObstacleSelection(obstacle);
			}
		}
	}
	
	private void initRequiredTiles(){
		requiredTileSelections.getChildren().clear();
		initStartTile();
		initEndTile();
	}
	
	private void initStartTile(){
		MapDao mapDao = new MapDao();
		TileImage startTileImage = mapDao.findTileImageByOwner(Constant.IMAGE_START_POINT_OWNER);
		
		startTileImageView = new ImageView(Util.byteArrayToImage(startTileImage.getImage()));
		startTileImageView.setFitHeight(Constant.TILE_HEIGHT);
		startTileImageView.setFitWidth(Constant.TILE_WIDTH);
		
		if(!isEditable){
			startTileImageView.setOnMouseClicked(new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent event) {
					selectedTileImageView.setImage(startTileImageView.getImage());
					selectedTileImage = startTileImage;
					selectedObstacle = null;
					
					startTileSelected = true;
					endTileSelected = false;
					
					selectedTileDescription.setText("Type: Start Point");
				}
			});
		}
		
		requiredTileSelections.getChildren().add(startTileImageView);
	}
	
	private void initEndTile(){
		MapDao mapDao = new MapDao();
		TileImage endTileImage = mapDao.findTileImageByOwner(Constant.IMAGE_END_POINT_OWNER);
		
		endTileImageView = new ImageView(Util.byteArrayToImage(endTileImage.getImage()));
		endTileImageView.setFitHeight(Constant.TILE_HEIGHT);
		endTileImageView.setFitWidth(Constant.TILE_WIDTH);
		
		if(!isEditable){
			endTileImageView.setOnMouseClicked(new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent event) {
					selectedTileImageView.setImage(endTileImageView.getImage());
					selectedTileImage = endTileImage;
					selectedObstacle = null;
					
					startTileSelected = false;
					endTileSelected = true;
					
					selectedTileDescription.setText("Type: End Point");
				}
			});
		}
		
		requiredTileSelections.getChildren().add(endTileImageView);
	}
	
	private void addObstacleSelection(Obstacle obstacle){
		ImageView imageView = new ImageView(Util.byteArrayToImage(obstacle.getImage()));
		imageView.setFitHeight(Constant.TILE_HEIGHT);
		imageView.setFitWidth(Constant.TILE_WIDTH);
		
		if(!isEditable){
			imageView.setOnMouseClicked(new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent event) {
					selectedTileImageView.setImage(imageView.getImage());
					selectedTileImage = null;
					selectedObstacle = obstacle;
					
					startTileSelected = false;
					endTileSelected = false;
					
					selectedTileDescription
						.setText("Type: Obstacle \n"
							+ "Name: " + obstacle.getName() + "\n"
							+ "Difficulty: " + obstacle.getDifficulty() + "\n"
							+ "Radius: " + obstacle.getRadius());
				}
			});
		}
		
		obstacleImageSelections.getChildren().add(imageView);
	}
	
	private void addTileImageSelection(TileImage tileImage){
		ImageView imageView = new ImageView(Util.byteArrayToImage(tileImage.getImage()));
		imageView.setFitHeight(Constant.TILE_HEIGHT);
		imageView.setFitWidth(Constant.TILE_WIDTH);
		
		if(!isEditable){
			imageView.setOnMouseClicked(new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent event) {
					handleTimeImageSelection(tileImage, imageView.getImage());
					
					if(event.getClickCount() == 2){
						showTileImageDialog(tileImage);
					}
					
					if (event.isControlDown()) {
						DropShadow ds = new DropShadow(20, Color.RED);
						imageView.setEffect(ds);
						selectedTileImages.add(tileImage);
						moveToArchiveButton.setVisible(true);
						viewArchivesButton.setVisible(false);
					} else {
						removeAllSelectedTileImage();
						moveToArchiveButton.setVisible(false);
						viewArchivesButton.setVisible(true);
					}
				}
			});
		}
		
		tileImageSelections.getChildren().add(imageView);
	}
	
	private void removeAllSelectedTileImage() {
		if (tileImageSelections.getChildren() != null) {
			for (Node node : tileImageSelections.getChildren()) {
				if (node instanceof ImageView) {
					node.setEffect(null);
				}
			}
			selectedTileImages = new ArrayList<TileImage>();
		}
	}
	
	private void handleTimeImageSelection(TileImage tileImage, Image image){
		selectedTileImageView.setImage(image);
		selectedTileImage = tileImage;
		selectedObstacle = null;
		
		startTileSelected = false;
		endTileSelected = false;
		
		selectedTileDescription
			.setText("Type: Tile \n"
				+ "File Name: " + tileImage.getFileName() + "\n"
				+ "Difficulty: " + tileImage.getDifficulty() + "\n"
				+ "Vertical Tilt: " + (tileImage.getVerticalTilt() != null ? tileImage.getVerticalTilt() : 0) + "\n"
				+ "Horizontal Tilt: " + (tileImage.getHorizontalTilt() != null ? tileImage.getHorizontalTilt() : 0) + "\n"
				+ "Radius: 0");
	}
	
	public void show(Action action, Game game,Alert loadingDialog,boolean copyFromOtherMap){
		try{
			FXMLLoader loader = new FXMLLoader();
	        loader.setLocation(getClass().getResource("Designer.fxml"));
	        AnchorPane designer = (AnchorPane) loader.load();
	        
	        designer.getStylesheets().add("/css/style.css");
	        stage = new Stage();
	        stage.initModality(Modality.WINDOW_MODAL);
	        stage.initOwner(MainLayout.getPrimaryStage());
	        stage.initStyle(StageStyle.UNDECORATED);
	        Scene scene = new Scene(designer);
	        stage.setMaximized(true);
	        stage.setScene(scene);
	        
	        MapDesignerController controller = (MapDesignerController) loader.getController();
	        controller.setGame(game);
	        controller.setStage(stage);
	        controller.setCopyFromOtherMap(copyFromOtherMap);
	        controller.setCurrentAction(action);
	        controller.init();
	        loadingDialog.close();
	        stage.showAndWait();
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	@FXML
	private void uploadTileImage(){
		showTileImageDialog(new TileImage());
	}
	
	private void showTileImageDialog(TileImage tileImage){
		boolean saveClicked = new TileImageDialogController().show(tileImage, stage);
	    if (saveClicked) {
	        new MapDao().saveOrUpdate(tileImage);
	        initTileImageSelections();
	        handleTimeImageSelection(tileImage, Util.byteArrayToImage(tileImage.getImage()));
	    }
	}
	
	private void showObstacleDialog(Tile tile){
		System.out.println("Before. Tile Obstacle Difficulty: " + tile.getObstacleDifficulty()
		+ ". Tile Obstacle Radius: " + tile.getObstacleRadius());
		
		boolean saveClicked = new ObstacleDialogController().show(tile, stage);
	    if (saveClicked) {
	        System.out.println("After. Tile Obstacle Difficulty: " + tile.getObstacleDifficulty()
	        		+ ". Tile Obstacle Radius: " + tile.getObstacleRadius());
	    }
	}
	
	@FXML
	private void saveButtonClicked(){
		if(getMap().getStartPoint() == null || getMap().getEndPoint() == null){
			new AlertDialog(AlertType.ERROR, "Invalid Map", null, "Map should have 1 start point and 1 end point.",stage).showAndWait();
		} else {
			new GamePropertiesController().show(currentAction, getGame(),false,copyFromOtherMap);
			stage.close();
		}
	}
	
	@FXML
	private void cancelButtonClicked(){
		if(!copyFromOtherMap){
			getMap().setTiles(null);
		}
		stage.close();
	}

	@FXML
	public void moveToArchive() {
		if (selectedTileImages != null) {
			MapDao mapDao = new MapDao();
			for (TileImage tileImage : selectedTileImages) {
				tileImage.setArchived(true);
				mapDao.saveOrUpdate(tileImage);
			}
			selectedTileImages.clear();
			initTileImageSelections();
			moveToArchiveButton.setVisible(false);
			viewArchivesButton.setVisible(true);
		}
	}
	
	@FXML
	public void viewArchives() {
		new ArchivedTilesDialogController().show(stage);
		initTileImageSelections();
	}
	
	public Map getMap() {
		return getGame().getMap();
	}
	
	public Game getGame(){
		return game;
	}
	
	public void setGame(Game game){
		this.game = game;
	}
	
	public void setStage(Stage stage) {
		this.stage = stage;
	}
	
	public void setViewTitle(String title){
		viewTitle.setText(title);
	}
	
	public Action getCurrentAction() {
		return currentAction;
	}
	
	public void setCurrentAction(Action currentAction) {
		this.currentAction = currentAction;
	}
	
	public void setCopyFromOtherMap(boolean copyFromOtherMap) {
		this.copyFromOtherMap = copyFromOtherMap;
	}
}
