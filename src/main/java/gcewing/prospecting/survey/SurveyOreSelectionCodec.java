package gcewing.prospecting.survey;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class SurveyOreSelectionCodec {

    private static final int FORMAT_VERSION = 1;
    private static final int MAX_ORE_NAMES = 2048;
    private static final int MAX_ORE_NAME_LENGTH = 128;

    private SurveyOreSelectionCodec() {}

    public static void writeState(DataOutput output, SurveyOreSelectionState state) throws IOException {
        if (output == null) throw new IllegalArgumentException("Output must not be null");
        if (state == null) throw new IllegalArgumentException("Survey ore selection state must not be null");
        output.writeByte(FORMAT_VERSION);
        writeSelectionBody(output, state.getSelection());
        writeNames(output, state.getSelectableOreNames());
    }

    public static SurveyOreSelectionState readState(DataInput input) throws IOException {
        requireVersion(input);
        SurveyOreSelection selection = readSelectionBody(input);
        return new SurveyOreSelectionState(selection, readNames(input));
    }

    public static void writeSelection(DataOutput output, SurveyOreSelection selection) throws IOException {
        if (output == null) throw new IllegalArgumentException("Output must not be null");
        output.writeByte(FORMAT_VERSION);
        writeSelectionBody(output, selection);
    }

    public static SurveyOreSelection readSelection(DataInput input) throws IOException {
        requireVersion(input);
        return readSelectionBody(input);
    }

    private static void requireVersion(DataInput input) throws IOException {
        if (input == null) throw new IllegalArgumentException("Input must not be null");
        int formatVersion = input.readUnsignedByte();
        if (formatVersion != FORMAT_VERSION)
            throw new IOException("Unsupported survey ore selection version: " + formatVersion);
    }

    private static void writeSelectionBody(DataOutput output, SurveyOreSelection selection) throws IOException {
        if (selection == null) throw new IllegalArgumentException("Survey ore selection must not be null");
        boolean showAll = selection.getMode() == SurveyOreSelection.Mode.SHOW_ALL;
        output.writeBoolean(showAll);
        writeNames(output, showAll ? java.util.Collections.<String>emptySet() : selection.getSelectedOreNames());
    }

    private static SurveyOreSelection readSelectionBody(DataInput input) throws IOException {
        boolean showAll = input.readBoolean();
        List<String> selectedOreNames = readNames(input);
        if (showAll && !selectedOreNames.isEmpty()) throw new IOException("Show-all selection must not list ore names");
        return showAll ? SurveyOreSelection.showAll() : SurveyOreSelection.custom(selectedOreNames);
    }

    private static void writeNames(DataOutput output, Iterable<String> oreNames) throws IOException {
        List<String> names = new ArrayList<String>();
        for (String oreName : oreNames) {
            validateName(oreName);
            names.add(oreName);
            if (names.size() > MAX_ORE_NAMES) throw new IOException("Too many ore names");
        }
        output.writeShort(names.size());
        for (String oreName : names) output.writeUTF(oreName);
    }

    private static List<String> readNames(DataInput input) throws IOException {
        int nameCount = input.readUnsignedShort();
        if (nameCount > MAX_ORE_NAMES) throw new IOException("Too many ore names: " + nameCount);
        List<String> names = new ArrayList<String>(nameCount);
        for (int i = 0; i < nameCount; i++) {
            String oreName = input.readUTF();
            validateName(oreName);
            names.add(oreName);
        }
        return names;
    }

    private static void validateName(String oreName) throws IOException {
        if (oreName == null || oreName.trim()
            .isEmpty()) throw new IOException("Ore name must not be empty");
        if (oreName.length() > MAX_ORE_NAME_LENGTH) throw new IOException("Ore name is too long");
    }
}
