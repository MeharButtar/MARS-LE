package mars.mips.instructions.customlangs;

import mars.simulator.*;
import mars.mips.hardware.*;
import mars.*;
import mars.util.*;
import mars.mips.instructions.*;

public class HogwartsAssembly extends CustomAssembly {

    @Override
    public String getName() {
        return "Hogwarts Assembly";
    }

    @Override
    public String getDescription() {
        return "Magical instruction set based on wizardry.";
    }

    @Override
    protected void populate() {

        // ----------------------------------------------------------
        //  BASIC ARITHMETIC (all R-format)
        // ----------------------------------------------------------

        // wingardium rd, rs, rt  (add)
        instructionList.add(new BasicInstruction(
                "wingardium $t0, $t1, $t2",
                "Add: $t0 = $t1 + $t2",
                BasicInstructionFormat.R_FORMAT,
                "000000 sssss ttttt ddddd 00000 100000",
                statement -> {
                    int[] o = statement.getOperands();
                    RegisterFile.updateRegister(o[0],
                            RegisterFile.getValue(o[1]) + RegisterFile.getValue(o[2]));
                }
        ));

        // diffindo rd, rs, rt (sub)
        instructionList.add(new BasicInstruction(
                "diffindo $t0, $t1, $t2",
                "Subtract: $t0 = $t1 - $t2",
                BasicInstructionFormat.R_FORMAT,
                "000000 sssss ttttt ddddd 00000 100010",
                statement -> {
                    int[] o = statement.getOperands();
                    RegisterFile.updateRegister(o[0],
                            RegisterFile.getValue(o[1]) - RegisterFile.getValue(o[2]));
                }
        ));

        // engorgio rd, rs, rt (mul)
        // FIX: funct 011000 is "mult" (HI/LO only). Use a custom unused funct instead.
        instructionList.add(new BasicInstruction(
                "engorgio $t0, $t1, $t2",
                "Multiply: $t0 = $t1 * $t2",
                BasicInstructionFormat.R_FORMAT,
                "000000 sssss ttttt ddddd 00000 011100",
                statement -> {
                    int[] o = statement.getOperands();
                    RegisterFile.updateRegister(o[0],
                            RegisterFile.getValue(o[1]) * RegisterFile.getValue(o[2]));
                }
        ));

        // finitelink rd, rs, rt (and)
        instructionList.add(new BasicInstruction(
                "finitelink $t0, $t1, $t2",
                "Bitwise AND",
                BasicInstructionFormat.R_FORMAT,
                "000000 sssss ttttt ddddd 00000 100100",
                statement -> {
                    int[] o = statement.getOperands();
                    RegisterFile.updateRegister(o[0],
                            RegisterFile.getValue(o[1]) & RegisterFile.getValue(o[2]));
                }
        ));

        // alohafuse rd, rs, rt (or)
        instructionList.add(new BasicInstruction(
                "alohafuse $t0, $t1, $t2",
                "Bitwise OR",
                BasicInstructionFormat.R_FORMAT,
                "000000 sssss ttttt ddddd 00000 100101",
                statement -> {
                    int[] o = statement.getOperands();
                    RegisterFile.updateRegister(o[0],
                            RegisterFile.getValue(o[1]) | RegisterFile.getValue(o[2]));
                }
        ));

        // alohomora rd (increment)
        // FIX: was "000000 fffff ..." which put the register in the rs slot.
        // Destination register must be in the rd (ddddd) slot for R-format.
        instructionList.add(new BasicInstruction(
                "alohomora $t0",
                "Increment register",
                BasicInstructionFormat.R_FORMAT,
                "000000 00000 00000 ddddd 00000 101111",
                statement -> {
                    int r = statement.getOperands()[0];
                    RegisterFile.updateRegister(r,
                            RegisterFile.getValue(r) + 1);
                }
        ));


        // ----------------------------------------------------------
        //  BRANCHING
        // ----------------------------------------------------------

        // legilimens rs, rt, label (beq)
        instructionList.add(new BasicInstruction(
                "legilimens $t0, $t1, label",
                "Branch if equal",
                BasicInstructionFormat.I_BRANCH_FORMAT,
                "000100 sssss ttttt iiiiiiiiiiiiiiii",
                statement -> {
                    int[] o = statement.getOperands();
                    if (RegisterFile.getValue(o[0]) == RegisterFile.getValue(o[1])) {
                        Globals.instructionSet.processBranch(o[2]);
                    }
                }
        ));

        // confundo rs, rt, label (bne)
        instructionList.add(new BasicInstruction(
                "confundo $t0, $t1, label",
                "Branch if not equal",
                BasicInstructionFormat.I_BRANCH_FORMAT,
                "000101 sssss ttttt iiiiiiiiiiiiiiii",
                statement -> {
                    int[] o = statement.getOperands();
                    if (RegisterFile.getValue(o[0]) != RegisterFile.getValue(o[1])) {
                        Globals.instructionSet.processBranch(o[2]);
                    }
                }
        ));

        // stupefy rs, label (branch if rs == 0)
        instructionList.add(new BasicInstruction(
                "stupefy $t0, label",
                "Branch if reg == 0",
                BasicInstructionFormat.I_BRANCH_FORMAT,
                "000100 sssss 00000 iiiiiiiiiiiiiiii",
                statement -> {
                    int[] o = statement.getOperands();
                    if (RegisterFile.getValue(o[0]) == 0) {
                        Globals.instructionSet.processBranch(o[1]);
                    }
                }
        ));


        // ----------------------------------------------------------
        //  MEMORY (Load/store MUST be exactly rt, offset(rs))
        // ----------------------------------------------------------

        // accio rt, offset(rs)  (lw)
        // FIX: template must use "offset(base)" syntax, not three separate operands.
        instructionList.add(new BasicInstruction(
                "accio $t0, 0($t1)",
                "Load word",
                BasicInstructionFormat.I_FORMAT,
                "100011 ttttt sssss iiiiiiiiiiiiiiii",
                statement -> {
                    int[] o = statement.getOperands(); // rt, imm, rs
                    int addr = RegisterFile.getValue(o[2]) + (o[1] << 16 >> 16);
                    try {
                        RegisterFile.updateRegister(o[0], Globals.memory.getWord(addr));
                    } catch (Exception e) {
                        throw new ProcessingException(statement, e.getMessage());
                    }
                }
        ));

        // reducto rt, offset(rs) (sw)
        // FIX: same template fix as accio.
        instructionList.add(new BasicInstruction(
                "reducto $t0, 0($t1)",
                "Store word",
                BasicInstructionFormat.I_FORMAT,
                "101011 ttttt sssss iiiiiiiiiiiiiiii",
                statement -> {
                    int[] o = statement.getOperands(); // rt, imm, rs
                    int addr = RegisterFile.getValue(o[2]) + (o[1] << 16 >> 16);
                    try {
                        Globals.memory.setWord(addr, RegisterFile.getValue(o[0]));
                    } catch (Exception e) {
                        throw new ProcessingException(statement, e.getMessage());
                    }
                }
        ));


        // ----------------------------------------------------------
        //  MAGIC SPELLS
        // ----------------------------------------------------------

        // lumos rd
        // FIX: funct 101010 = slt, 101011 = sltu — both conflict with MIPS.
        // Changed to unused functs 110001 and 110010.
        instructionList.add(new BasicInstruction(
                "lumos $t0",
                "Set register = 1",
                BasicInstructionFormat.R_FORMAT,
                "000000 00000 00000 ddddd 00000 110001",
                statement -> {
                    RegisterFile.updateRegister(statement.getOperands()[0], 1);
                }
        ));

        // nox rd
        instructionList.add(new BasicInstruction(
                "nox $t0",
                "Set register = 0",
                BasicInstructionFormat.R_FORMAT,
                "000000 00000 00000 ddddd 00000 110010",
                statement -> {
                    RegisterFile.updateRegister(statement.getOperands()[0], 0);
                }
        ));

        // crucio rd (rd -= 10)
        instructionList.add(new BasicInstruction(
                "crucio $t0",
                "Damage spell (subtract 10)",
                BasicInstructionFormat.R_FORMAT,
                "000000 00000 00000 ddddd 00000 101110",
                statement -> {
                    int r = statement.getOperands()[0];
                    RegisterFile.updateRegister(r,
                            RegisterFile.getValue(r) - 10);
                }
        ));

        // patronus rd (rd *= 42)
        instructionList.add(new BasicInstruction(
                "patronus $t0",
                "Protection spell (multiply by 42)",
                BasicInstructionFormat.R_FORMAT,
                "000000 00000 00000 ddddd 00000 110000",
                statement -> {
                    int r = statement.getOperands()[0];
                    RegisterFile.updateRegister(r,
                            RegisterFile.getValue(r) * 42);
                }
        ));

        // polyjuice rd, rs (rd = ~rs)
        instructionList.add(new BasicInstruction(
                "polyjuice $t0, $t1",
                "Copy and invert bits",
                BasicInstructionFormat.R_FORMAT,
                "000000 sssss 00000 ddddd 00000 101101",
                statement -> {
                    int[] o = statement.getOperands();
                    RegisterFile.updateRegister(o[0], ~RegisterFile.getValue(o[1]));
                }
        ));

        // expelliarmus rs, rt  (if equal, rt = 0)
        instructionList.add(new BasicInstruction(
                "expelliarmus $t0, $t1",
                "Disarm: if equal, set second register to 0",
                BasicInstructionFormat.R_FORMAT,
                "000000 sssss ttttt 00000 00000 101100",
                statement -> {
                    int[] o = statement.getOperands();
                    if (RegisterFile.getValue(o[0]) == RegisterFile.getValue(o[1])) {
                        RegisterFile.updateRegister(o[1], 0);
                    }
                }
        ));
    }
}