package stellar.plugin.type;

import arc.struct.ObjectIntMap;
import arc.struct.ObjectMap;
import arc.util.Log;
import arc.util.Strings;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.core.NetServer;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.net.Packets;
import stellar.database.DatabaseAsync;
import stellar.database.gen.tables.records.UsersRecord;
import stellar.plugin.Const;
import stellar.plugin.Variables;
import stellar.plugin.components.bot.Bot;
import stellar.plugin.components.bot.Colors;
import stellar.plugin.components.bot.Util;
import thedimas.util.Bundle;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import static stellar.plugin.util.StringUtils.longToTime;

public class VoteSession {
    public Player initiator;
    public Player target;
    public ObjectIntMap<String> voted = new ObjectIntMap<>();
    public Timer.Task task;
    public int votes;
    public String reason;

    public VoteSession(Player initiator, Player target, String reason) {
        this.initiator = initiator;
        this.target = target;
        this.reason = reason;
        this.task = Timer.schedule(() -> {
            if (!this.checkPass()) {
                Bundle.bundled("commands.votekick.failed", target.coloredName());
                Variables.voteSession = null;
                this.task.cancel();
                getMessage()
                        .thenAcceptAsync(s -> Bot.sendEmbed(Const.votekickChannel, Util.embedBuilder("Голосование провалилось", s, Colors.blue)))
                        .exceptionally(t -> {
                            Log.err(t);
                            return null;
                        });
            }
        }, NetServer.voteDuration);
    }

    private CompletableFuture<String> getMessage() { // uses async database calls
        ObjectMap<String, UsersRecord> infos = new ObjectMap<>();
        return CompletableFuture.allOf(
                voted.keys()
                        .toArray()
                        .map(s -> DatabaseAsync.getPlayerAsync(s)
                                .thenAcceptAsync(p -> infos.put(p.getUuid(), p)))
                        .toArray(CompletableFuture.class)
        ).thenComposeAsync(ignored ->
                DatabaseAsync.getPlayerAsync(target.uuid())
        ).thenApplyAsync(info -> {
            infos.put(target.uuid(), info);
            String votedFor = String.join("; ", infos.values()
                    .toSeq()
                    .select(i -> voted.get(i.getUuid()) == 1)
                    .map(i -> "%s (%d)".formatted(Strings.stripColors(i.getName()), i.getId())));
            votedFor = votedFor.isEmpty() ? "`<Никто>`" : votedFor;
            String votedAgainst = String.join("; ", infos.values()
                    .toSeq()
                    .select(i -> voted.get(i.getUuid()) == -1)
                    .map(i -> "%s (%d)".formatted(Strings.stripColors(i.getName()), i.getId())));
            votedAgainst = votedAgainst.isEmpty() ? "`<Никто>`" : votedAgainst;
            return """
                    **Сервер**: %s
                    **Инициатор**: %s (%s)
                    **Цель**: %s (%s)
                    **За**: %s
                    **Против**: %s
                    **Причина**: %s
                    """.formatted(Const.serverFieldName, initiator.plainName(), infos.get(initiator.uuid()).getId(), target.plainName(), infos.get(target.uuid()).getId(), votedFor, votedAgainst, reason);
        });
    }

    public void vote(Player player, int d) {
        int lastVote = this.voted.get(player.uuid(), 0);
        this.votes -= lastVote;
        this.votes += d;
        this.voted.put(player.uuid(), d);
        Bundle.bundled("commands.votekick.voted", player.coloredName(), target.coloredName(), this.votes, Vars.netServer.votesRequired());
        this.checkPass();
    }

    public boolean checkPass() {
        if (this.votes >= Vars.netServer.votesRequired()) {
            Groups.player.each(p -> {
                Locale locale = Bundle.findLocale(p.locale());
                Bundle.bundled("commands.votekick.passed", target.coloredName(), longToTime(NetServer.kickDuration, locale));
            });
            Groups.player.each(p -> p.uuid().equals(this.target.uuid()), (p) -> {
                p.kick(Packets.KickReason.vote, NetServer.kickDuration * 1000L);
            });
            Variables.voteSession = null;
            getMessage()
                    .thenAcceptAsync(s -> Bot.sendEmbed(Const.votekickChannel, Util.embedBuilder("Голосование успешно", s, Colors.red)))
                    .exceptionally(t -> {
                        Log.err(t);
                        return null;
                    });
            this.task.cancel();
            return true;
        } else {
            return false;
        }
    }
}