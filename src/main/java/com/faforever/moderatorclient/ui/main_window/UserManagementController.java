package com.faforever.moderatorclient.ui.main_window;

import com.faforever.moderatorclient.api.dto.*;
import com.faforever.moderatorclient.api.rest.domain.UserService;
import com.faforever.moderatorclient.mapstruct.GamePlayerStatsMapper;
import com.faforever.moderatorclient.mapstruct.PlayerMapper;
import com.faforever.moderatorclient.ui.*;
import com.faforever.moderatorclient.ui.domain.FeaturedModFX;
import com.faforever.moderatorclient.ui.domain.GamePlayerStatsFX;
import com.faforever.moderatorclient.ui.domain.PlayerFX;
import com.faforever.moderatorclient.ui.domain.UserNoteFX;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class UserManagementController implements Controller<SplitPane> {
    private final UiService uiService;
    private final PlatformService platformService;
    private final UserService userService;
    private final PlayerMapper playerMapper;
    private final GamePlayerStatsMapper gamePlayerStatsMapper;

    private final ObservableList<UserNoteFX> userNotes;

    private final String replayDownLoadFormat;

    public SplitPane root;

    public RadioButton searchUserByIdRadioButton;
    public RadioButton searchUserByCurrentNameRadioButton;
    public RadioButton searchUserByPreviousNamesRadioButton;
    public RadioButton searchUserByEmailRadioButton;
    public RadioButton searchUserBySteamIdRadioButton;
    public RadioButton searchUserByIpRadioButton;
    public TextField userSearchTextField;
    public TableView<UserNoteFX> userNoteTableView;
    public Button addNoteButton;
    public Button editNoteButton;
    public Button newBanButton;
    public Button editBanButton;
    public TableView<Player> userSearchTableView;
    public TableView<NameRecord> userNameHistoryTableView;
    public TableView<BanInfo> userBansTableView;
    public TableView<Teamkill> userTeamkillsTableView;
    public TableView<AvatarAssignment> userAvatarsTableView;
    public TableView<GamePlayerStatsFX> userLastGamesTable;
    public ChoiceBox<FeaturedModFX> featuredModFilterChoiceBox;
    public Button loadMoreGamesButton;
    private Runnable loadMoreGamesRunnable;
    private int userGamesPage = 1;

    public UserManagementController(UiService uiService, PlatformService platformService, UserService userService, PlayerMapper playerMapper, GamePlayerStatsMapper gamePlayerStatsMapper, @Value("${faforever.vault.replayDownloadUrlFormat}") String replayDownLoadFormat) {
        this.uiService = uiService;
        this.platformService = platformService;
        this.userService = userService;
        this.playerMapper = playerMapper;
        this.gamePlayerStatsMapper = gamePlayerStatsMapper;
        this.replayDownLoadFormat = replayDownLoadFormat;
        userNotes = FXCollections.observableArrayList();
    }

    @Override
    public SplitPane getRoot() {
        return root;
    }

    @FXML
    public void initialize() {
        ViewHelper.buildUserTableView(userSearchTableView);
        ViewHelper.buildNotesTableView(userNoteTableView, userNotes, false);
        ViewHelper.buildNameHistoryTableView(userNameHistoryTableView);
        ViewHelper.buildBanTableView(userBansTableView);
        ViewHelper.buildPlayersGamesTable(userLastGamesTable, replayDownLoadFormat, platformService);

        addNoteButton.disableProperty().bind(userSearchTableView.getSelectionModel().selectedItemProperty().isNull());
        editNoteButton.disableProperty().bind(userNoteTableView.getSelectionModel().selectedItemProperty().isNull());

        loadMoreGamesButton.visibleProperty()
                .bind(Bindings.createBooleanBinding(() -> userLastGamesTable.getItems().size() != 0 && userLastGamesTable.getItems().size() % 100 == 0, userLastGamesTable.getItems()));

        featuredModFilterChoiceBox.setConverter(new StringConverter<FeaturedModFX>() {
            @Override
            public String toString(FeaturedModFX object) {
                return object == null ? "All" : object.getDisplayName();
            }

            @Override
            public FeaturedModFX fromString(String string) {
                throw (new UnsupportedOperationException("Not implemented"));
            }
        });
        featuredModFilterChoiceBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            userLastGamesTable.getItems().clear();
            userGamesPage = 1;
            if (loadMoreGamesRunnable != null) loadMoreGamesRunnable.run();
        });

        featuredModFilterChoiceBox.getItems().add(null);
        featuredModFilterChoiceBox.getSelectionModel().select(0);
        CompletableFuture.supplyAsync(userService::getFeaturedMods)
                .thenAccept(featuredMods -> Platform.runLater(() -> featuredModFilterChoiceBox.getItems().addAll(featuredMods)));

        ViewHelper.buildTeamkillTableView(userTeamkillsTableView, false);
        ViewHelper.buildUserAvatarsTableView(userAvatarsTableView);

        userSearchTableView.getSelectionModel().selectedItemProperty().addListener(this::onSelectedUser);
        editBanButton.disableProperty().bind(userBansTableView.getSelectionModel().selectedItemProperty().isNull());
    }

    private void onSelectedUser(ObservableValue<? extends Player> observable, Player oldValue, Player newValue) {
        userNoteTableView.getItems().clear();
        userNameHistoryTableView.getItems().clear();
        userNameHistoryTableView.getSortOrder().clear();
        userBansTableView.getItems().clear();
        userBansTableView.getSortOrder().clear();
        userTeamkillsTableView.getItems().clear();
        userTeamkillsTableView.getSortOrder().clear();
        userAvatarsTableView.getItems().clear();
        userAvatarsTableView.getSortOrder().clear();
        userLastGamesTable.getItems().clear();
        userLastGamesTable.getSortOrder().clear();

        userNoteTableView.getSortOrder().clear();
        userNotes.clear();

        if (newValue != null) {
            userNotes.addAll(userService.getUserNotes(newValue.getId()));
            userNameHistoryTableView.getItems().addAll(newValue.getNames());
            userBansTableView.getItems().addAll(newValue.getBans());
            userTeamkillsTableView.getItems().addAll(userService.findTeamkillsByUserId(newValue.getId()));
            userAvatarsTableView.getItems().addAll(newValue.getAvatarAssignments());

            userGamesPage = 1;
            loadMoreGamesRunnable = () -> CompletableFuture.supplyAsync(() -> gamePlayerStatsMapper.map(userService.getLastHundredPlayedGamesByFeaturedMod(newValue.getId(), userGamesPage, featuredModFilterChoiceBox.getSelectionModel().getSelectedItem())))
                    .thenAccept(gamePlayerStats -> Platform.runLater(() -> userLastGamesTable.getItems().addAll(gamePlayerStats)));
            loadMoreGamesRunnable.run();
        }

        newBanButton.setDisable(newValue == null);
    }

    public void onUserSearch() {
        userSearchTableView.getItems().clear();
        userSearchTableView.getSortOrder().clear();

        Collection<Player> usersFound = Collections.emptyList();
        String searchPattern = userSearchTextField.getText();
        if (searchUserByIdRadioButton.isSelected()) {
            usersFound = userService.findUserById(searchPattern);
        } else if (searchUserByCurrentNameRadioButton.isSelected()) {
            usersFound = userService.findUserByName(searchPattern);
        } else if (searchUserByPreviousNamesRadioButton.isSelected()) {
            usersFound = userService.findUsersByPreviousName(searchPattern);
        } else if (searchUserByEmailRadioButton.isSelected()) {
            usersFound = userService.findUserByEmail(searchPattern);
        } else if (searchUserBySteamIdRadioButton.isSelected()) {
            usersFound = userService.findUserBySteamId(searchPattern);
        } else if (searchUserByIpRadioButton.isSelected()) {
            usersFound = userService.findUserByIP(searchPattern);
        }

        userSearchTableView.getItems().addAll(usersFound);
    }

    public void onNewBan() {
        Player selectedPlayer = userSearchTableView.getSelectionModel().getSelectedItem();
        Assert.notNull(selectedPlayer, "You need to select a player to create a ban.");

        BanInfoController banInfoController = uiService.loadFxml("ui/banInfo.fxml");
        banInfoController.setBanInfo(new BanInfo()
                .setPlayer(selectedPlayer)
        );

        Stage banInfoDialog = new Stage();
        banInfoDialog.setTitle("Apply new ban");
        banInfoDialog.setScene(new Scene(banInfoController.getRoot()));
        banInfoDialog.showAndWait();
    }

    public void onEditBan() {
        BanInfo selectedBan = userBansTableView.getSelectionModel().getSelectedItem();
        Assert.notNull(selectedBan, "You need to select a ban to edit it.");

        BanInfoController banInfoController = uiService.loadFxml("ui/banInfo.fxml");
        banInfoController.setBanInfo(selectedBan);

        Stage banInfoDialog = new Stage();
        banInfoDialog.setTitle("Edit Ban");
        banInfoDialog.setScene(new Scene(banInfoController.getRoot()));
        banInfoDialog.showAndWait();
    }

    public void loadMoreGames() {
        userGamesPage++;
        loadMoreGamesRunnable.run();
    }

    public void addNote() {
        PlayerFX selectedPlayer = playerMapper.map(userSearchTableView.getSelectionModel().getSelectedItem());
        Assert.notNull(selectedPlayer, "You need to select a player to create a userNote.");

        UserNoteController userNoteController = uiService.loadFxml("ui/userNote.fxml");

        UserNoteFX userNoteFX = new UserNoteFX();
        userNoteFX.setPlayer(selectedPlayer);
        userNoteController.addPostedListener(userNotes::add);

        userNoteController.setUserNoteFX(userNoteFX);

        Stage userNoteDialog = new Stage();
        userNoteDialog.setTitle("Add new player note");
        userNoteDialog.setScene(new Scene(userNoteController.getRoot()));
        userNoteDialog.showAndWait();
    }

    public void editNote() {
        UserNoteFX selectedUserNote = userNoteTableView.getSelectionModel().getSelectedItem();
        Assert.notNull(selectedUserNote, "You need to select a player note to edit it.");

        UserNoteController userNoteController = uiService.loadFxml("ui/userNote.fxml");
        userNoteController.setUserNoteFX(selectedUserNote);

        Stage userNoteDialog = new Stage();
        userNoteDialog.setTitle("Edit player note");
        userNoteDialog.setScene(new Scene(userNoteController.getRoot()));
        userNoteDialog.showAndWait();
    }
}
