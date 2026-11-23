/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */

package io.github.dsheirer.gui.settings.thinline;

import io.github.dsheirer.audio.broadcast.thinline.ThinlineRadioSocket;
import io.github.dsheirer.gui.settings.SettingsPage;
import io.github.dsheirer.gui.settings.SettingsPageType;
import io.github.dsheirer.gui.settings.SettingsSection;
import io.github.dsheirer.settings.Settings;
import io.github.dsheirer.settings.thinline.ThinlineRadioPreference;
import io.github.dsheirer.util.ThreadPool;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import org.controlsfx.control.ToggleSwitch;

/**
 * Thinline Radio site health reporting and the remote control permissions for Thinline Radio admins.
 */
public class ThinlineRadioSettingsPage extends SettingsPage
{
    private final ThinlineRadioPreference mPreference;

    /**
     * Constructs an instance
     * @param settings for the Thinline Radio settings
     */
    public ThinlineRadioSettingsPage(Settings settings)
    {
        super(SettingsPageType.THINLINE_RADIO, settings);
        mPreference = settings.getThinlineRadioPreference();

        SettingsSection connection = addSection("Connection");
        connection.addHelp("Opens one websocket to Thinline Radio. This SDRTrunk sends site health and upload " +
            "check-ins. Thinline Radio admins can control this SDRTrunk remotely, limited by the choices at the " +
            "bottom of this page.");

        connection.addRow("Report site health to Thinline Radio",
            toggle(mPreference.isEnabled(), mPreference::setEnabled));

        TextField url = new TextField(mPreference.getUrl());
        url.setPromptText("https://scanner.example.com");
        url.setPrefColumnCount(28);
        url.focusedProperty().addListener((observable, oldValue, focused) -> {
            if(!focused)
            {
                mPreference.setUrl(url.getText());
            }
        });
        connection.addRow("Thinline Radio URL", url);

        PasswordField apiKey = new PasswordField();
        apiKey.setText(mPreference.getApiKey());
        apiKey.setPromptText("API key used only for site health");
        apiKey.setPrefColumnCount(28);
        apiKey.focusedProperty().addListener((observable, oldValue, focused) -> {
            if(!focused)
            {
                mPreference.setApiKey(apiKey.getText());
            }
        });
        connection.addRow("Health API key", apiKey);

        Button test = new Button("Test Connection");
        Label result = new Label();
        result.setWrapText(true);
        test.setOnAction(event -> {
            mPreference.setUrl(url.getText());
            mPreference.setApiKey(apiKey.getText());
            String host = url.getText();
            String key = apiKey.getText();
            test.setDisable(true);
            result.setText("Connecting…");
            ThreadPool.CACHED.submit(() -> {
                String error = ThinlineRadioSocket.testConnection(host, key);
                Platform.runLater(() -> {
                    test.setDisable(false);
                    result.setText(error == null ? "Connected." : error);
                });
            });
        });
        HBox testRow = new HBox(8, result, test);
        testRow.setAlignment(Pos.CENTER_RIGHT);
        connection.addRow("Connection test", testRow);

        SettingsSection alerts = addSection("Push These Errors");
        alerts.addRow("Tuner error", toggle(mPreference.isAlertTuner(), mPreference::setAlertTuner));
        alerts.addRow("Control channel lost", toggle(mPreference.isAlertControl(), mPreference::setAlertControl));
        alerts.addRow("Uploader error", toggle(mPreference.isAlertUploader(), mPreference::setAlertUploader));
        alerts.addRow("Sample overflow", toggle(mPreference.isAlertOverflow(), mPreference::setAlertOverflow));
        alerts.addRow("SDRTrunk offline", toggle(mPreference.isAlertOffline(), mPreference::setAlertOffline));

        SettingsSection remote = addSection("Let Thinline Radio Admins");
        remote.addHelp("Status and settings are always visible. Thinline Radio can only change what is turned on " +
            "here; nothing is turned on until you allow it.");

        for(ThinlineRadioPreference.Remote permission: ThinlineRadioPreference.Remote.values())
        {
            remote.addRow(permission.toString(), toggle(mPreference.isRemoteAllowed(permission),
                allowed -> mPreference.setRemoteAllowed(permission, allowed)));
        }
    }

    private static ToggleSwitch toggle(boolean selected, Consumer<Boolean> save)
    {
        ToggleSwitch toggle = new ToggleSwitch();
        toggle.setSelected(selected);
        toggle.selectedProperty().addListener((observable, oldValue, value) -> save.accept(value));
        return toggle;
    }
}
