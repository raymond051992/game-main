package com.creatingskies.game.config.company;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;

import com.creatingskies.game.classes.TableViewController;
import com.creatingskies.game.common.MainLayout;
import com.creatingskies.game.component.AlertDialog;
import com.creatingskies.game.core.GameDao;
import com.creatingskies.game.model.IRecord;
import com.creatingskies.game.model.company.Company;
import com.creatingskies.game.model.company.CompanyDAO;
import com.creatingskies.game.model.company.Group;
import com.creatingskies.game.model.company.Team;
import com.creatingskies.game.model.event.GameEvent;
import com.creatingskies.game.model.event.GameEventDao;

public class CompanyController extends TableViewController{

	@FXML private TableView<Company> companyTableView;
	@FXML private TableColumn<Company, String> companyNameTableColumn;
	@FXML private TableColumn<Company, Object> companyActionTableColumn;
	
	@FXML private FlowPane groupsFlowPane;
	@FXML private Button addGroupButton;
	@FXML private Button archiveButton;
	@FXML private CheckBox showArchives;
	
	private Company selectedCompany;
	
	@Override
	public void init() {
		archiveButton.setVisible(false);
		super.init();
		initTable();
		loadCompanies();
		addGroupButton.setVisible(false);
		showArchives.selectedProperty().addListener(new ChangeListener<Boolean>() {
			public void changed(javafx.beans.value.ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				loadCompanies();
				archiveButton.setVisible(!companyTableView.getItems().isEmpty());
			};
		});
	}
	
	@SuppressWarnings("unchecked")
	private void initTable(){
		companyNameTableColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
				cellData.getValue().getName()));
		
		companyActionTableColumn.setCellFactory(generateCellFactory(Action.EDIT,Action.DELETE));
		
		companyTableView.getSelectionModel().selectedItemProperty()
    	.addListener(
    			(observable, oldValue, newValue) -> showDetails(newValue));
		
	}
	
	private void loadCompanies(){
		companyTableView.setItems(FXCollections.observableArrayList(new CompanyDAO().findAllCompanies(showArchives.isSelected())));
	}
	
	@Override
	protected void deleteRecord(IRecord record) {
		List<GameEvent> events = new GameEventDao().findAllGameEventByCompany((Company) record); 
		
		if(events == null || (events != null && events.isEmpty())){
			Optional<ButtonType> result = new AlertDialog(AlertType.CONFIRMATION, "Confirmation Dialog",
					"Are you sure you want to delete this company?", null).showAndWait();
			
			if(result.get() == ButtonType.OK){
				super.deleteRecord(record);
				try {
					new GameDao().delete(record);
					loadCompanies();
					showDetails(null);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}else{
			new AlertDialog(AlertType.ERROR, "Error", "", "The action can't be completed because the file is currently active in a game").showAndWait();
		}
	}
	
	public void addNewGroup(){
		Group group = new Group();
		group.setCompany(selectedCompany);
		if(new GroupDialogController().show(group)){
			showDetails(selectedCompany);
		}
	}
	
	public void showDetails(Company company){
		groupsFlowPane.getChildren().clear();
		if(company != null){
			List<Group> groups = new CompanyDAO().findAllGroupsForCompany(company,showArchives.isSelected());
			for(Group group : groups){
				group.setTeams(new CompanyDAO().findAllTeamsForGroup(group));
				for(Team team : group.getTeams()){
					team.setPlayers(new CompanyDAO().findAllPlayersForTeam(team));
				}
				groupsFlowPane.getChildren().add(new CompanyGroupPane(group));
			}
			selectedCompany = company;
			archiveButton.setVisible(true);
			if(company.getArchived() == null || company.getArchived() == Boolean.FALSE){
				archiveButton.setText("Archive");
			}else{
				archiveButton.setText("Restore");
			}
			addGroupButton.setVisible(true);
		}else{
			selectedCompany = company;
			addGroupButton.setVisible(false);
		}
	}
	
	@Override
	protected String getViewTitle() {
		return "Companies";
	}
	
	public void handleAdd() {
		if(new CompanyDialogController().show(new Company())){
			loadCompanies();
		}
	}
	
	@Override
	protected void editRecord(IRecord record) {
		if(new CompanyDialogController().show(selectedCompany)){
			loadCompanies();
		}
	}
	
	public void show(){
		try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("Company.fxml"));
            AnchorPane companies = (AnchorPane) loader.load();
            MainLayout.getRootLayout().setCenter(companies);
            
            CompanyController controller = loader.getController();
            controller.init();
        } catch (IOException e) {
            e.printStackTrace();
        }
	}

	@Override
	public TableView<? extends IRecord> getTableView() {
		return companyTableView;
	}
	
	@FXML
	public void moveToArchive(){
		if(selectedCompany != null){
			if(selectedCompany.getArchived() == null || selectedCompany.getArchived() == Boolean.FALSE){
				selectedCompany.setArchived(Boolean.TRUE);
				archiveButton.setText("Restore");
			}else{
				selectedCompany.setArchived(Boolean.FALSE);
				archiveButton.setText("Archive");
			}
			CompanyDAO companyDAO = new CompanyDAO();
			companyDAO.saveOrUpdate(selectedCompany);
			init();
		}
	}
}
