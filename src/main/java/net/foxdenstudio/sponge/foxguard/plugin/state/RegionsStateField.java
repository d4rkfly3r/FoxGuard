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

package net.foxdenstudio.sponge.foxguard.plugin.state;

import com.google.common.collect.ImmutableList;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.AdvCmdParse;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.ProcessResult;
import net.foxdenstudio.sponge.foxcore.plugin.state.ListStateFieldBase;
import net.foxdenstudio.sponge.foxguard.plugin.FGManager;
import net.foxdenstudio.sponge.foxguard.plugin.object.IFGObject;
import net.foxdenstudio.sponge.foxguard.plugin.region.IRegion;
import net.foxdenstudio.sponge.foxguard.plugin.util.FGHelper;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.GuavaCollectors;
import org.spongepowered.api.util.StartsWithPredicate;
import org.spongepowered.api.world.World;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import static net.foxdenstudio.sponge.foxcore.plugin.util.Aliases.WORLD_ALIASES;
import static net.foxdenstudio.sponge.foxcore.plugin.util.Aliases.isIn;

public class RegionsStateField extends ListStateFieldBase<IRegion> {

    public static final String ID = "region";

    private static final Function<Map<String, String>, Function<String, Consumer<String>>> MAPPER = map -> key -> value -> {
        map.put(key, value);
        if (isIn(WORLD_ALIASES, key) && !map.containsKey("world")) {
            map.put("world", value);
        }
    };

    public RegionsStateField() {
        super("Regions");
    }

    @Override
    public Text currentState() {
        Text.Builder builder = Text.builder();
        Iterator<IRegion> regionIterator = this.list.iterator();
        int index = 1;
        while (regionIterator.hasNext()) {
            IRegion region = regionIterator.next();
            builder.append(Text.of(FGHelper.getColorForRegion(region),
                    (index++) + ": " + region.getShortTypeName() + " : " + region.getWorld().getName() + " : " + region.getName()));
            if (regionIterator.hasNext()) builder.append(Text.of("\n"));
        }
        return builder.build();
    }

    @Override
    public ProcessResult modify(CommandSource source, String arguments) throws CommandException {
        AdvCmdParse.ParseResult parse = AdvCmdParse.builder().arguments(arguments).limit(1).parseLastFlags(false).parse();
        String newArgs = parse.args.length > 1 ? parse.args[1] : "";
        if (parse.args.length == 0 || parse.args[0].equalsIgnoreCase("add")) {
            return add(source, newArgs);
        } else if (parse.args[0].equalsIgnoreCase("remove")) {
            return remove(source, newArgs);
        }
        return ProcessResult.of(false, Text.of("Not a valid region state command!"));
    }

    @Override
    public List<String> modifySuggestions(CommandSource source, String arguments) throws CommandException {
        AdvCmdParse.ParseResult parse = AdvCmdParse.builder()
                .arguments(arguments)
                .flagMapper(MAPPER)
                .excludeCurrent(true)
                .autoCloseQuotes(true)
                .parse();
        if (parse.current.type.equals(AdvCmdParse.CurrentElement.ElementType.ARGUMENT)) {
            if (parse.current.index == 0) {
                return ImmutableList.of("add", "remove").stream()
                        .filter(new StartsWithPredicate(parse.current.token))
                        .collect(GuavaCollectors.toImmutableList());
            } else if (parse.current.index == 1) {
                String worldName = parse.flagmap.get("world");
                World world = null;
                if (source instanceof Player) world = ((Player) source).getWorld();
                if (!worldName.isEmpty()) {
                    Optional<World> optWorld = Sponge.getGame().getServer().getWorld(worldName);
                    if (optWorld.isPresent()) {
                        world = optWorld.get();
                    }
                }
                if (world == null) return ImmutableList.of();
                if (parse.args[0].equals("add")) {
                    return FGManager.getInstance().getRegionsList(world).stream()
                            .filter(region -> !this.list.contains(region))
                            .map(IFGObject::getName)
                            .filter(new StartsWithPredicate(parse.current.token))
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                } else if (parse.args[0].equals("remove")) {
                    final World finalWorld = world;
                    return this.list.stream()
                            .filter(region -> region.getWorld().equals(finalWorld))
                            .map(IFGObject::getName)
                            .filter(new StartsWithPredicate(parse.current.token))
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                }

            }
        } else if (parse.current.type.equals(AdvCmdParse.CurrentElement.ElementType.LONGFLAGKEY))
            return ImmutableList.of("world").stream()
                    .filter(new StartsWithPredicate(parse.current.token))
                    .map(args -> parse.current.prefix + args)
                    .collect(GuavaCollectors.toImmutableList());
        else if (parse.current.type.equals(AdvCmdParse.CurrentElement.ElementType.LONGFLAGVALUE)) {
            if (isIn(WORLD_ALIASES, parse.current.key))
                return Sponge.getGame().getServer().getWorlds().stream()
                        .map(World::getName)
                        .filter(new StartsWithPredicate(parse.current.token))
                        .map(args -> parse.current.prefix + args)
                        .collect(GuavaCollectors.toImmutableList());
        } else if (parse.current.type.equals(AdvCmdParse.CurrentElement.ElementType.COMPLETE))
            return ImmutableList.of(parse.current.prefix + " ");
        return ImmutableList.of();
    }

    public ProcessResult add(CommandSource source, String arguments) throws CommandException {
        AdvCmdParse.ParseResult parse = AdvCmdParse.builder().arguments(arguments).flagMapper(MAPPER).parse();

        if (parse.args.length < 1) throw new CommandException(Text.of("Must specify a name!"));
        String worldName = parse.flagmap.get("world");
        World world = null;
        if (source instanceof Player) world = ((Player) source).getWorld();
        if (!worldName.isEmpty()) {
            Optional<World> optWorld = Sponge.getGame().getServer().getWorld(worldName);
            if (optWorld.isPresent()) {
                world = optWorld.get();
            }
        }
        if (world == null) throw new CommandException(Text.of("Must specify a world!"));
        IRegion region = FGManager.getInstance().getRegion(world, parse.args[0]);
        if (region == null)
            throw new CommandException(Text.of("No Regions with the name\"" + parse.args[0] + "\"!"));
        if (this.list.contains(region))
            throw new CommandException(Text.of("Region is already in your state buffer!"));
        this.list.add(region);

        return ProcessResult.of(true, Text.of("Successfully added Region to your state buffer!"));
    }

    public ProcessResult remove(CommandSource source, String arguments) throws CommandException {
        AdvCmdParse.ParseResult parse = AdvCmdParse.builder().arguments(arguments).flagMapper(MAPPER).parse();

        if (parse.args.length < 1) throw new CommandException(Text.of("Must specify a name or a number!"));
        String worldName = parse.flagmap.get("world");
        World world = null;
        if (source instanceof Player) world = ((Player) source).getWorld();
        if (!worldName.isEmpty()) {
            Optional<World> optWorld = Sponge.getGame().getServer().getWorld(worldName);
            if (optWorld.isPresent()) {
                world = optWorld.get();
            }
        }
        IRegion region;
        try {
            int index = Integer.parseInt(parse.args[0]);
            region = this.list.get(index - 1);
        } catch (NumberFormatException e) {
            if (world == null) throw new CommandException(Text.of("Must specify a world!"));
            region = FGManager.getInstance().getRegion(world, parse.args[0]);
        } catch (IndexOutOfBoundsException e) {
            throw new CommandException(Text.of("Index " + parse.args[0] + " out of bounds! (1 - "
                    + this.list.size()));
        }
        if (region == null)
            throw new CommandException(Text.of("No Regions with the name\"" + parse.args[0] + "\"!"));
        if (!this.list.contains(region))
            throw new CommandException(Text.of("Region is not in your state buffer!"));
        this.list.remove(region);

        return ProcessResult.of(true, Text.of("Successfully removed Region from your state buffer!"));
    }
}
