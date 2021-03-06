/*
 * This file is part of FoxGuard, licensed under the MIT License (MIT).
 *
 * Copyright (c) gravityfox - https://gravityfox.net/
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.foxdenstudio.sponge.foxguard.plugin.handler;

import com.google.common.collect.ImmutableList;
import net.foxdenstudio.sponge.foxcore.common.FCHelper;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.AdvCmdParse;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.ProcessResult;
import net.foxdenstudio.sponge.foxcore.plugin.util.CallbackHashMap;
import net.foxdenstudio.sponge.foxguard.plugin.Flag;
import net.foxdenstudio.sponge.foxguard.plugin.FoxGuardMain;
import net.foxdenstudio.sponge.foxguard.plugin.listener.util.EventResult;
import net.foxdenstudio.sponge.foxguard.plugin.object.IMembership;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.source.ProxySource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.GuavaCollectors;
import org.spongepowered.api.util.StartsWithPredicate;
import org.spongepowered.api.util.Tristate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import static net.foxdenstudio.sponge.foxcore.plugin.util.Aliases.*;

public class SimpleHandler extends OwnableHandlerBase implements IMembership {

    private final Map<Flag, Tristate> ownerPermissions;
    private final Map<Flag, Tristate> memberPermissions;
    private final Map<Flag, Tristate> defaultPermissions;
    private PassiveOptions passiveOption = PassiveOptions.PASSTHROUGH;
    private List<User> memberList = new ArrayList<>();

    public SimpleHandler(String name, int priority) {
        this(name, priority,
                new CallbackHashMap<>((o, m) -> Tristate.UNDEFINED),
                new CallbackHashMap<>((o, m) -> Tristate.UNDEFINED),
                new CallbackHashMap<>((o, m) -> Tristate.UNDEFINED));
    }

    public SimpleHandler(String name, int priority,
                         Map<Flag, Tristate> ownerPermissions,
                         Map<Flag, Tristate> memberPermissions,
                         Map<Flag, Tristate> defaultPermissions) {
        super(name, priority);
        this.ownerPermissions = ownerPermissions;
        this.memberPermissions = memberPermissions;
        this.defaultPermissions = defaultPermissions;
    }

    @Override
    public ProcessResult modify(CommandSource source, String arguments) throws CommandException {
        if (!source.hasPermission("foxguard.command.modify.objects.modify.handlers")) {
            if (source instanceof ProxySource) source = ((ProxySource) source).getOriginalSource();
            if (source instanceof Player && !this.ownerList.contains(source)) return ProcessResult.failure();
        }
        AdvCmdParse.ParseResult parse = AdvCmdParse.builder().arguments(arguments).parse();
        if (parse.args.length > 0) {
            if (isIn(GROUPS_ALIASES, parse.args[0])) {
                if (parse.args.length > 1) {
                    List<User> list;
                    if (isIn(OWNER_GROUP_ALIASES, parse.args[1])) {
                        list = this.ownerList;
                    } else if (isIn(MEMBER_GROUP_ALIASES, parse.args[1])) {
                        list = this.memberList;
                    } else {
                        return ProcessResult.of(false, Text.of(TextColors.RED, "Not a valid group!"));
                    }
                    if (parse.args.length > 2) {
                        UserOperations op;
                        if (parse.args[2].equalsIgnoreCase("add")) {
                            op = UserOperations.ADD;
                        } else if (parse.args[2].equalsIgnoreCase("remove")) {
                            op = UserOperations.REMOVE;
                        } else if (parse.args[2].equalsIgnoreCase("set")) {
                            op = UserOperations.SET;
                        } else {
                            return ProcessResult.of(false, Text.of("Not a valid operation!"));
                        }
                        if (parse.args.length > 3) {
                            int successes = 0;
                            int failures = 0;
                            List<String> names = new ArrayList<>();
                            Collections.addAll(names, Arrays.copyOfRange(parse.args, 3, parse.args.length));
                            List<User> argUsers = new ArrayList<>();
                            for (String name : names) {
                                Optional<User> optUser = FoxGuardMain.instance().getUserStorage().get(name);
                                if (optUser.isPresent() && !FCHelper.isUserOnList(argUsers, optUser.get()))
                                    argUsers.add(optUser.get());
                                else failures++;
                            }
                            switch (op) {
                                case ADD:
                                    for (User user : argUsers) {
                                        if (!FCHelper.isUserOnList(list, user) && list.add(user))
                                            successes++;
                                        else failures++;
                                    }
                                    break;
                                case REMOVE:
                                    for (User user : argUsers) {
                                        if (FCHelper.isUserOnList(list, user)) {
                                            list.remove(user);
                                            successes++;
                                        } else failures++;
                                    }
                                    break;
                                case SET:
                                    list.clear();
                                    for (User user : argUsers) {
                                        list.add(user);
                                        successes++;
                                    }
                            }
                            return ProcessResult.of(true, Text.of("Modified list with " + successes + " successes and " + failures + " failures."));
                        } else {
                            return ProcessResult.of(false, Text.of("Must specify one or more users!"));
                        }
                    } else {
                        return ProcessResult.of(false, Text.of("Must specify an operation!"));
                    }
                } else {
                    return ProcessResult.of(false, Text.of("Must specify a group!"));
                }
            } else if (isIn(SET_ALIASES, parse.args[0])) {
                Map<Flag, Tristate> map;
                if (parse.args.length > 1) {
                    if (isIn(OWNER_GROUP_ALIASES, parse.args[1])) {
                        map = ownerPermissions;
                    } else if (isIn(MEMBER_GROUP_ALIASES, parse.args[1])) {
                        map = memberPermissions;
                    } else if (isIn(DEFAULT_GROUP_ALIASES, parse.args[1])) {
                        map = defaultPermissions;
                    } else {

                        return ProcessResult.of(false, Text.of("Not a valid group!"));
                    }
                } else {
                    return ProcessResult.of(false, Text.of("Must specify a group!"));
                }
                if (parse.args.length > 2) {
                    Flag flag;
                    if (parse.args[2].equalsIgnoreCase("all")) {
                        flag = null;
                    } else {
                        flag = Flag.flagFrom(parse.args[2]);
                        if (flag == null) {
                            return ProcessResult.of(false, Text.of("Not a valid flag!"));
                        }
                    }
                    if (parse.args.length > 3) {
                        if (isIn(CLEAR_ALIASES, parse.args[3])) {
                            if (flag == null) {
                                map.clear();
                                return ProcessResult.of(true, Text.of("Successfully cleared flags!"));
                            } else {
                                map.remove(flag);
                                return ProcessResult.of(true, Text.of("Successfully cleared flag!"));
                            }
                        } else {
                            Tristate tristate = tristateFrom(parse.args[3]);
                            if (tristate == null) {
                                return ProcessResult.of(false, Text.of("Not a valid value!"));
                            }
                            if (flag == null) {
                                for (Flag thatExist : Flag.values()) {
                                    map.put(thatExist, tristate);
                                }
                                return ProcessResult.of(true, Text.of("Successfully set flags!"));
                            } else {
                                map.put(flag, tristate);
                                return ProcessResult.of(true, Text.of("Successfully set flag!"));
                            }
                        }
                    } else {
                        return ProcessResult.of(false, Text.of("Must specify a value!"));
                    }
                } else {
                    return ProcessResult.of(false, Text.of("Must specify a flag!"));
                }
            } else if (isIn(PASSIVE_ALIASES, parse.args[0])) {
                if (parse.args.length > 1) {
                    if (isIn(TRUE_ALIASES, parse.args[1])) {
                        this.passiveOption = PassiveOptions.ALLOW;
                        return ProcessResult.of(true, Text.of("Successfully set passive option!"));
                    } else if (isIn(FALSE_ALIASES, parse.args[1])) {
                        this.passiveOption = PassiveOptions.DENY;
                        return ProcessResult.of(true, Text.of("Successfully set passive option!"));
                    } else if (isIn(PASSTHROUGH_ALIASES, parse.args[1])) {
                        this.passiveOption = PassiveOptions.PASSTHROUGH;
                        return ProcessResult.of(true, Text.of("Successfully set passive option!"));
                    } else if (isIn(OWNER_GROUP_ALIASES, parse.args[1])) {
                        this.passiveOption = PassiveOptions.OWNER;
                        return ProcessResult.of(true, Text.of("Successfully set passive option!"));
                    } else if (isIn(MEMBER_GROUP_ALIASES, parse.args[1])) {
                        this.passiveOption = PassiveOptions.MEMBER;
                        return ProcessResult.of(true, Text.of("Successfully set passive option!"));
                    } else if (isIn(DEFAULT_GROUP_ALIASES, parse.args[1])) {
                        this.passiveOption = PassiveOptions.DEFAULT;
                        return ProcessResult.of(true, Text.of("Successfully set passive option!"));
                    } else {
                        return ProcessResult.of(false, Text.of("Not a valid option!"));
                    }
                } else {
                    return ProcessResult.of(false, Text.of("Must specify an option!"));
                }
            } else {
                return ProcessResult.of(false, Text.of("Not a valid SimpleHandler command!"));
            }
        } else {
            return ProcessResult.of(false, Text.of("Must specify a command!"));
        }
    }

    @Override
    public List<String> modifySuggestions(CommandSource source, String arguments) throws CommandException {
        AdvCmdParse.ParseResult parse = AdvCmdParse.builder()
                .arguments(arguments)
                .excludeCurrent(true)
                .autoCloseQuotes(true)
                .parse();
        if (parse.current.type.equals(AdvCmdParse.CurrentElement.ElementType.ARGUMENT)) {
            if (parse.current.index == 0) {
                return ImmutableList.of("set", "group", "passive").stream()
                        .filter(new StartsWithPredicate(parse.current.token))
                        .map(args -> parse.current.prefix + args)
                        .collect(GuavaCollectors.toImmutableList());
            } else if (parse.current.index == 1) {
                if (isIn(GROUPS_ALIASES, parse.args[0]) || isIn(SET_ALIASES, parse.args[0])) {
                    return ImmutableList.of("owner", "member").stream()
                            .filter(new StartsWithPredicate(parse.current.token))
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                } else if (isIn(PASSIVE_ALIASES, parse.args[0])) {
                    return ImmutableList.of("true", "false", "passthrough", "owner", "member", "default").stream()
                            .filter(new StartsWithPredicate(parse.current.token))
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                }
            } else if (parse.current.index == 2) {
                if (isIn(GROUPS_ALIASES, parse.args[0])) {
                    return ImmutableList.of("add", "remove", "set").stream()
                            .filter(new StartsWithPredicate(parse.current.token))
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                } else if (isIn(SET_ALIASES, parse.args[0])) {
                    return Arrays.stream(Flag.values())
                            .map(Flag::flagName)
                            .filter(new StartsWithPredicate(parse.current.token))
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                }
            } else if (parse.current.index == 3) {
                if (isIn(GROUPS_ALIASES, parse.args[0])) {
                    List<User> list;
                    if (isIn(OWNER_GROUP_ALIASES, parse.args[1])) {
                        list = this.ownerList;
                    } else if (isIn(MEMBER_GROUP_ALIASES, parse.args[1])) {
                        list = this.memberList;
                    } else {
                        return ImmutableList.of();
                    }
                    if (parse.args[2].equalsIgnoreCase("set")) {
                        return Sponge.getGame().getServer().getOnlinePlayers().stream()
                                .map(Player::getName)
                                .filter(new StartsWithPredicate(parse.current.token))
                                .map(args -> parse.current.prefix + args)
                                .collect(GuavaCollectors.toImmutableList());
                    } else if (parse.args[2].equalsIgnoreCase("add")) {
                        return Sponge.getGame().getServer().getOnlinePlayers().stream()
                                .filter(player -> !FCHelper.isUserOnList(list, player))
                                .map(Player::getName)
                                .filter(new StartsWithPredicate(parse.current.token))
                                .map(args -> parse.current.prefix + args)
                                .collect(GuavaCollectors.toImmutableList());
                    } else if (parse.args[2].equalsIgnoreCase("remove")) {
                        return Sponge.getGame().getServer().getOnlinePlayers().stream()
                                .filter(player -> FCHelper.isUserOnList(list, player))
                                .map(Player::getName)
                                .filter(new StartsWithPredicate(parse.current.token))
                                .map(args -> parse.current.prefix + args)
                                .collect(GuavaCollectors.toImmutableList());
                    }
                } else if (isIn(SET_ALIASES, parse.args[0])) {
                    return ImmutableList.of("true", "false", "passthrough", "clear").stream()
                            .filter(new StartsWithPredicate(parse.current.token))
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                }
            } else if (parse.current.index > 3) {
                if (isIn(GROUPS_ALIASES, parse.args[0])) {
                    List<User> list;
                    if (isIn(OWNER_GROUP_ALIASES, parse.args[1])) {
                        list = this.ownerList;
                    } else if (isIn(MEMBER_GROUP_ALIASES, parse.args[1])) {
                        list = this.memberList;
                    } else {
                        return ImmutableList.of();
                    }
                    if (parse.args[2].equalsIgnoreCase("set")) {
                        return Sponge.getGame().getServer().getOnlinePlayers().stream()
                                .map(Player::getName)
                                .filter(new StartsWithPredicate(parse.current.token))
                                .map(args -> parse.current.prefix + args)
                                .collect(GuavaCollectors.toImmutableList());
                    } else if (parse.args[2].equalsIgnoreCase("add")) {
                        return Sponge.getGame().getServer().getOnlinePlayers().stream()
                                .filter(player -> !FCHelper.isUserOnList(list, player))
                                .map(Player::getName)
                                .filter(alias -> !isIn(Arrays.copyOfRange(parse.args, 2, parse.args.length), alias))
                                .filter(new StartsWithPredicate(parse.current.token))
                                .map(args -> parse.current.prefix + args)
                                .collect(GuavaCollectors.toImmutableList());
                    } else if (parse.args[2].equalsIgnoreCase("remove")) {
                        return Sponge.getGame().getServer().getOnlinePlayers().stream()
                                .filter(player -> FCHelper.isUserOnList(list, player))
                                .map(Player::getName)
                                .filter(alias -> !isIn(Arrays.copyOfRange(parse.args, 2, parse.args.length), alias))
                                .filter(new StartsWithPredicate(parse.current.token))
                                .map(args -> parse.current.prefix + args)
                                .collect(GuavaCollectors.toImmutableList());
                    }
                }
            }
        } else if (parse.current.type.equals(AdvCmdParse.CurrentElement.ElementType.COMPLETE))
            return ImmutableList.of(parse.current.prefix + " ");
        return ImmutableList.of();
    }

    @Override
    public EventResult handle(User user, Flag flag, Event event) {
        if (user == null) {
            switch (this.passiveOption) {
                case OWNER:
                    return EventResult.of(getResult(this.ownerPermissions, flag));
                case MEMBER:
                    return EventResult.of(getResult(this.memberPermissions, flag));
                case DEFAULT:
                    return EventResult.of(getResult(this.defaultPermissions, flag));
                case ALLOW:
                    return EventResult.allow();
                case DENY:
                    return EventResult.deny();
                case PASSTHROUGH:
                    return EventResult.pass();
            }
        }
        if (FCHelper.isUserOnList(this.ownerList, user)) return EventResult.of(getResult(this.ownerPermissions, flag));
        else if (FCHelper.isUserOnList(this.memberList, user))
            return EventResult.of(getResult(this.memberPermissions, flag));
        else return EventResult.of(getResult(this.defaultPermissions, flag));
    }

    private Tristate getResult(Map<Flag, Tristate> map, Flag flag) {
        Flag temp = flag;
        while (temp != null && !map.containsKey(temp)) {
            temp = temp.getParent();
        }
        if (temp != null) return map.get(temp);
        else return map.get(flag);
    }

    @Override
    public String getShortTypeName() {
        return "Simple";
    }

    @Override
    public String getLongTypeName() {
        return "Simple";
    }

    @Override
    public String getUniqueTypeString() {
        return "simple";
    }

    @Override
    public Text getDetails(String arguments) {
        Text.Builder builder = super.getDetails(arguments).toBuilder();
        builder.append(Text.of("\n"));
        builder.append(Text.of(TextColors.GREEN,
                TextActions.suggestCommand("/foxguard modify handler " + this.name + " group members add "),
                TextActions.showText(Text.of("Click to Add a Player(s) to Members")),
                "Members: "));
        for (User u : this.memberList) {
            builder.append(Text.of(TextColors.RESET,
                    TextActions.suggestCommand("/foxguard modify handler " + this.name + " group members remove " + u.getName()),
                    TextActions.showText(Text.of("Click to Remove Player \"" + u.getName() + "\" from Members")),
                    u.getName())).append(Text.of("  "));
        }
        builder.append(Text.of("\n"));
        builder.append(Text.of(TextColors.GOLD,
                TextActions.suggestCommand("/foxguard modify handler " + this.name + " set owners "),
                TextActions.showText(Text.of("Click to Set a Flag")),
                "Owner permissions:\n"));
        for (Flag f : this.ownerPermissions.keySet()) {
            builder.append(
                    Text.builder().append(Text.of("  " + f.toString() + ": "))
                            .append(FCHelper.readableTristateText(ownerPermissions.get(f)))
                            .append(Text.of("\n"))
                            .onClick(TextActions.suggestCommand("/foxguard modify handler " + this.name + " set owners " + f.flagName() + " "))
                            .onHover(TextActions.showText(Text.of("Click to Change This Flag")))
                            .build()
            );
        }
        builder.append(Text.of(TextColors.GREEN,
                TextActions.suggestCommand("/foxguard modify handler " + this.name + " set members "),
                TextActions.showText(Text.of("Click to Set a Flag")),
                "Member permissions:\n"));
        for (Flag f : this.memberPermissions.keySet()) {
            builder.append(
                    Text.builder().append(Text.of("  " + f.toString() + ": "))
                            .append(FCHelper.readableTristateText(memberPermissions.get(f)))
                            .append(Text.of("\n"))
                            .onClick(TextActions.suggestCommand("/foxguard modify handler " + this.name + " set members " + f.flagName() + " "))
                            .onHover(TextActions.showText(Text.of("Click to Change This Flag")))
                            .build()
            );
        }
        builder.append(Text.of(TextColors.RED,
                TextActions.suggestCommand("/foxguard modify handler " + this.name + " set default "),
                TextActions.showText(Text.of("Click to Set a Flag")),
                "Default permissions:\n"));
        for (Flag f : this.defaultPermissions.keySet()) {
            builder.append(
                    Text.builder().append(Text.of("  " + f.toString() + ": "))
                            .append(FCHelper.readableTristateText(defaultPermissions.get(f)))
                            .append(Text.of("\n"))
                            .onClick(TextActions.suggestCommand("/foxguard modify handler " + this.name + " set default " + f.flagName() + " "))
                            .onHover(TextActions.showText(Text.of("Click to Change This Flag")))
                            .build()
            );
        }
        builder.append(Text.builder()
                        .append(Text.of(TextColors.AQUA, "Passive setting: "))
                        .append(Text.of(TextColors.RESET, this.passiveOption.toString()))
                        .onClick(TextActions.suggestCommand("/foxguard modify handler " + this.name + " passive "))
                        .onHover(TextActions.showText(Text.of("Click to Change Passive Setting"))).build()
        );
        return builder.build();
    }

    @Override
    public void writeToDatabase(DataSource dataSource) throws SQLException {
        super.writeToDatabase(dataSource);
        try (Connection conn = dataSource.getConnection()) {
            try (Statement statement = conn.createStatement()) {
                statement.execute("CREATE TABLE IF NOT EXISTS MEMBERS(NAMES VARCHAR(256), USERUUID UUID);" +
                        "DELETE FROM MEMBERS;");
                try (PreparedStatement insert = conn.prepareStatement("INSERT INTO MEMBERS(NAMES, USERUUID) VALUES (?, ?)")) {
                    for (User member : memberList) {
                        insert.setString(1, member.getName());
                        insert.setObject(2, member.getUniqueId());
                        insert.addBatch();
                    }
                    insert.executeBatch();
                }
                statement.execute("CREATE TABLE IF NOT EXISTS MAP(KEY VARCHAR (256), VALUE VARCHAR (256));" +
                        "DELETE FROM MAP;");
                statement.execute("INSERT INTO MAP(KEY, VALUE) VALUES ('passive', '" + this.passiveOption.name() + "')");

                statement.execute("CREATE TABLE IF NOT EXISTS OWNERFLAGMAP(KEY VARCHAR (256), VALUE VARCHAR (256));" +
                        "DELETE FROM OWNERFLAGMAP;");
                statement.execute("CREATE TABLE IF NOT EXISTS MEMBERFLAGMAP(KEY VARCHAR (256), VALUE VARCHAR (256));" +
                        "DELETE FROM MEMBERFLAGMAP;");
                statement.execute("CREATE TABLE IF NOT EXISTS DEFAULTFLAGMAP(KEY VARCHAR (256), VALUE VARCHAR (256));" +
                        "DELETE FROM DEFAULTFLAGMAP;");
            }
            try (PreparedStatement statement = conn.prepareStatement("INSERT INTO OWNERFLAGMAP(KEY, VALUE) VALUES (? , ?)")) {
                for (Map.Entry<Flag, Tristate> entry : ownerPermissions.entrySet()) {
                    statement.setString(1, entry.getKey().name());
                    statement.setString(2, entry.getValue().name());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
            try (PreparedStatement statement = conn.prepareStatement("INSERT INTO MEMBERFLAGMAP(KEY, VALUE) VALUES (? , ?)")) {
                for (Map.Entry<Flag, Tristate> entry : memberPermissions.entrySet()) {
                    statement.setString(1, entry.getKey().name());
                    statement.setString(2, entry.getValue().name());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
            try (PreparedStatement statement = conn.prepareStatement("INSERT INTO DEFAULTFLAGMAP(KEY, VALUE) VALUES (? , ?)")) {
                for (Map.Entry<Flag, Tristate> entry : defaultPermissions.entrySet()) {
                    statement.setString(1, entry.getKey().name());
                    statement.setString(2, entry.getValue().name());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
        }
    }

    @Override
    protected String getAddOwnerSuggestion() {
        return "/foxguard modify handler " + this.getName() + " group owners add ";
    }

    @Override
    protected String getRemoveOwnerSuggestion(User user) {
        return "/foxguard modify handler " + this.getName() + " group owners remove " + user.getName();
    }

    @Override
    public List<User> getMembers() {
        return this.memberList;
    }

    @Override
    public void setMembers(List<User> members) {
        this.memberList = members;
    }

    @Override
    public boolean addMember(User player) {
        return memberList.add(player);
    }

    @Override
    public boolean removeMember(User player) {
        return memberList.remove(player);
    }

    public PassiveOptions getPassiveOption() {
        return passiveOption;
    }

    public void setPassiveOption(PassiveOptions passiveOption) {
        this.passiveOption = passiveOption;
    }

    public enum PassiveOptions {
        ALLOW, DENY, PASSTHROUGH, OWNER, MEMBER, DEFAULT;

        public String toString() {
            switch (this) {
                case ALLOW:
                    return "Allow";
                case DENY:
                    return "Deny";
                case PASSTHROUGH:
                    return "Passthrough";
                case OWNER:
                    return "Owner";
                case MEMBER:
                    return "Member";
                case DEFAULT:
                    return "Default";
                default:
                    return "Awut...?";
            }
        }
    }
}
