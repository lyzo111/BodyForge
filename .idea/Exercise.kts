class Exercise {
    var name: String,
    var type: String,
    var category: String,
    var targetMuscles: MutableList<String>,
    var secondaryMuscles: MutableList<String>,
    var targetBodyParts: MutableList<String>,
    var secondaryBodyParts: MutableList<String>,
    var equipment: MutableList<String>,
    var instructions: String

    public Exercise(
    String name, String type, String category, MutableList<String> targetMuscles, MutableList<String> targetBodyParts, String equipment, String instructions)
    {
        this.name = name,
        this.type = type,
        this.category = category
        this.targetMuscles = targetMuscles,
        this.bodyParts = targetBodyParts,
        this.equipment = equipment,
        this.instructions = instructions
    }

    public Exercise(
    String name, String type, String category, MutableList<String> targetMuscles, MutableList<String> secondaryMuscles, MutableList<String> targetBodyParts, MutableList<String> secondaryBodyParts, String equipment, String instructions)
    {
        this.name = name,
        this.type = type,
        this.category = category
        this.targetMuscles = targetMuscles,
        this.secondaryMuscles = secondaryMuscles,
        this.targetBodyParts = targetBodyParts,
        this.secondaryBodyParts = secondaryBodyParts,
        this.equipment = equipment,
        this.instructions = instructions
    }
}