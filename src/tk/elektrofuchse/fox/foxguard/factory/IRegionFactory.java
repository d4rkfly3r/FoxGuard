package tk.elektrofuchse.fox.foxguard.factory;

import org.spongepowered.api.util.command.CommandException;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.world.World;
import tk.elektrofuchse.fox.foxguard.commands.util.InternalCommandState;
import tk.elektrofuchse.fox.foxguard.regions.IRegion;

import javax.sql.DataSource;

/**
 * Created by Fox on 10/22/2015.
 */

public interface IRegionFactory extends IFGFactory {

    IRegion createRegion(String name, String type, String arguments, InternalCommandState state, World world, CommandSource source) throws CommandException;

    IRegion createRegion(DataSource source, String type);

}
