package tk.elektrofuchse.fox.foxguard.factory;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.command.CommandException;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.world.World;
import tk.elektrofuchse.fox.foxguard.commands.util.FGHelper;
import tk.elektrofuchse.fox.foxguard.commands.util.InternalCommandState;
import tk.elektrofuchse.fox.foxguard.regions.IRegion;
import tk.elektrofuchse.fox.foxguard.regions.RectRegion;

import javax.sql.DataSource;

/**
 * Created by Fox on 10/25/2015.
 */
public class FGRegionFactory implements IRegionFactory {

    String[] rectAliases = {"rectangular", "rectangle", "rect"};

    @Override
    public IRegion createRegion(String name, String type, String arguments, InternalCommandState state, World world, CommandSource source) throws CommandException {
        if (FGHelper.contains(rectAliases, type)) {
            if(source instanceof Player)
                return new RectRegion(name, state.positions, arguments.split(" "), source, (Player)source);
            else return new RectRegion(name, state.positions, arguments.split(" "), source );
        } else return null;
    }

    @Override
    public IRegion createRegion(DataSource source, String type) {
        return null;
    }

    @Override
    public String[] getAliases() {
        return FGHelper.concatAll(rectAliases);
    }

    @Override
    public String[] getTypes() {
        return new String[0];
    }
}
