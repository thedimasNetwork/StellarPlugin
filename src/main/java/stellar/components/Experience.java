package stellar.components;

import arc.Events;
import arc.util.Log;
import mindustry.game.EventType;
import mindustry.game.Gamemode;
import mindustry.game.Team;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import stellar.database.DBHandler;
import stellar.database.tables.Tables;
import stellar.database.tables.Users;

import java.sql.SQLException;

import static mindustry.Vars.state;
import static stellar.Variables.waves;

public class Experience {
    public static void load() {
        Events.on(EventType.GameOverEvent.class, event -> {
            Gamemode gamemode = state.rules.mode();
            Team winner = event.winner;
            switch (gamemode) {
                case pvp -> {
                    for (Player p : Groups.player) {
                        int currExp;
                        try {
                            currExp = DBHandler.get(p.uuid(), Tables.users.exp, Tables.users);
                        } catch (SQLException e) {
                            Log.err(e);
                            continue;
                        }
                        if (p.team() == winner) {
                            try {
                                DBHandler.update(p.uuid(), Tables.users.exp, Tables.users, currExp + 1000);
                            } catch (SQLException | NullPointerException e) {
                                Log.err(e);
                            }
                        } else if (p.team() != Team.derelict) {
                            try {
                                DBHandler.update(p.uuid(), Tables.users.exp, Tables.users, (currExp < 200) ? currExp - 200 : 0);
                            } catch (SQLException | NullPointerException e) {
                                Log.err(e);
                            }
                        }
                    }
                }
                case attack -> {
                    for (Player p : Groups.player) {
                        int currExp;
                        try {
                            currExp = DBHandler.get(p.uuid(), Tables.users.exp, Tables.users);
                        } catch (SQLException e) {
                            Log.err(e);
                            continue;
                        }
                        try {
                            if (winner == Team.sharded) {
                                DBHandler.update(p.uuid(), Tables.users.exp, Tables.users, currExp + 500);
                            } else if (winner != Team.derelict) {
                                DBHandler.update(p.uuid(), Tables.users.exp, Tables.users, (currExp < 100) ? currExp - 100 : 0);
                            }
                        } catch (SQLException e) {
                            Log.err(e);
                        }
                    }
                }
                case survival -> {
                    if (waves > 25) {
                        for (Player p : Groups.player) {
                            int currExp;
                            try {
                                currExp = DBHandler.get(p.uuid(), Tables.users.exp, Tables.users);
                                DBHandler.update(p.uuid(), Tables.users.exp, Tables.users, currExp + waves * 10);
                            } catch (SQLException e) {
                                Log.err(e);
                            }
                        }
                    }
                }
            }
            waves = 0;
        });

        Events.on(EventType.WaveEvent.class, event -> {
            waves++;
            Gamemode gamemode = state.rules.mode();
            switch (gamemode) {
                case survival -> {
                    for (Player p : Groups.player) {
                        int currExp;
                        try {
                            currExp = DBHandler.get(p.uuid(), Tables.users.exp, Tables.users);
                            DBHandler.update(p.uuid(), Tables.users.exp, Tables.users, currExp + 10 * waves % 10 == 0 ? 10 : 1);
                        } catch (SQLException e) {
                            Log.err(e);
                        }
                    }
                }
                case attack -> {
                    if (!(waves > 10)) {
                        break;
                    }
                    for (Player p : Groups.player) {
                        int currExp;
                        try {
                            currExp = DBHandler.get(p.uuid(), Tables.users.exp, Tables.users);
                            DBHandler.update(p.uuid(), Tables.users.exp, Tables.users, (currExp < 10) ? currExp - 10 : 0);
                        } catch (SQLException e) {
                            Log.err(e);
                        }
                    }
                }
            }
        });
    }
}
