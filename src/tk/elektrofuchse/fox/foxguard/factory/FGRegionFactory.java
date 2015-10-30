package tk.elektrofuchse.fox.foxguard.factory;

import com.flowpowered.math.vector.Vector2i;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.command.CommandException;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.world.World;
import tk.elektrofuchse.fox.foxguard.commands.util.InternalCommandState;
import tk.elektrofuchse.fox.foxguard.regions.IRegion;
import tk.elektrofuchse.fox.foxguard.regions.RectRegion;
import tk.elektrofuchse.fox.foxguard.regions.util.BoundingBox2;
import tk.elektrofuchse.fox.foxguard.util.FGHelper;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by Fox on 10/25/2015.
 * Project: foxguard
 */
public class FGRegionFactory implements IRegionFactory {

    String[] rectAliases = {"rectangular", "rectangle", "rect"};
    String[] types = {"rectangular"};

    @Override
    public IRegion createRegion(String name, String type, String arguments, InternalCommandState state, World world, CommandSource source) throws CommandException {
        if (FGHelper.contains(rectAliases, type)) {
            if (source instanceof Player)
                return new RectRegion(name, state.positions, arguments.split(" "), source, (Player) source);
            else return new RectRegion(name, state.positions, arguments.split(" "), source);
        } else return null;
    }

    @Override
    public IRegion createRegion(DataSource source, String name, String type) throws SQLException {
        if (type.equalsIgnoreCase("rectangular")) {
            Vector2i a;
            Vector2i b;
            try (Connection conn = source.getConnection()) {
                Statement statement = conn.createStatement();
                ResultSet set = statement.executeQuery("SELECT * FROM BOUNDS");
                set.next();
                a = new Vector2i(set.getInt("X"), set.getInt("Y"));
                set.next();
                b = new Vector2i(set.getInt("X"), set.getInt("Y"));
            }
            RectRegion region = new RectRegion(name, new BoundingBox2(a, b));

            return region;
        } else return null;
    }

    @Override
    public String[] getAliases() {
        return FGHelper.concatAll(rectAliases);
    }

    @Override
    public String[] getTypes() {
        return types;
    }
}